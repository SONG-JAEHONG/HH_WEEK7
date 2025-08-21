package kr.hhplus.be.server.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.infra.persistence.ConcertDateJpaRepository;
import kr.hhplus.be.server.concert.infra.persistence.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infra.persistence.SeatJpaRepository;
import kr.hhplus.be.server.concert.infra.redis.RedisKeys;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import kr.hhplus.be.server.payment.infra.persistence.PaymentJpaRepository;
import kr.hhplus.be.server.payment.infra.web.dto.PaymentRequest;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.infra.persistence.ReservationJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infra.persistence.UserJpaRepository;
import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import kr.hhplus.be.server.waiting.port.in.WaitingQueuePromoteUseCase;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "queue.promote.capacity=10",
        "queue.promote.max-batch=1000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentQueueCleanupIT {

    @Autowired MockMvc mockMvc;

    @Autowired ConcertCommandUseCase concertCommandUseCase;
    @Autowired ConcertJpaRepository concertRepo;
    @Autowired ConcertDateJpaRepository concertDateRepo;
    @Autowired SeatJpaRepository seatRepo;
    @Autowired ReservationJpaRepository reservationRepo;
    @Autowired PaymentJpaRepository paymentRepo;
    @Autowired UserJpaRepository userRepo;

    @Autowired WaitingQueueIssueUseCase issueUseCase;
    @Autowired WaitingQueuePromoteUseCase promoteUseCase;

    @Autowired StringRedisTemplate redis;

    private static final ObjectMapper om = new ObjectMapper();

    @AfterEach
    void clean() {

        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        paymentRepo.deleteAllInBatch();
        reservationRepo.deleteAllInBatch();
        seatRepo.deleteAllInBatch();
        concertDateRepo.deleteAllInBatch();
        concertRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("결제 커밋 후: working에서 ZREM, ticket.state=done, active_token 해제")
    void pay_then_cleanup_queue_after_commit() throws Exception {

        int totalSeats = 150;
        LocalDate date = LocalDate.of(2025, 9, 1);
        LocalDateTime openAt = LocalDateTime.of(2025, 8, 20, 9, 0, 0);

        var createReq = new CreateConcertRequest(
                "테스트콘서트",
                java.util.List.of(new CreateConcertRequest.Date(date, openAt, totalSeats))
        );
        var createRes = concertCommandUseCase.createConcert(createReq);
        Long concertDateId = createRes.concertDateIds().get(0);

        var concertDate = concertDateRepo.findById(concertDateId).orElseThrow();

        String totalKey = RedisKeys.TotalSeat(concertDateId);
        String remainKey = RedisKeys.RemainSeat(concertDateId);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(totalKey)).isEqualTo(String.valueOf(totalSeats));
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo(String.valueOf(totalSeats));
        });


        Seat seat = seatRepo.save(
                Seat.builder()
                        .concertDate(concertDate)
                        .number(1)
                        .status(SeatStatus.HOLDING)
                        .build()
        );
        User user = userRepo.save(User.builder().point(10_000L).build());
        Reservation reservation = reservationRepo.save(
                Reservation.builder()
                        .user(user)
                        .concertDate(concertDate)
                        .seat(seat)
                        .status(ReservationStatus.HOLDING)
                        .build()
        );


        IssueTicketResponse ticket = issueUseCase.issueTicket(user.getId());

        promoteUseCase.promoteOnce();


        assertThat(redis.opsForZSet().rank(WaitingQueueKeys.workingZ(), ticket.token()))
                .as("working zset membership").isNotNull();

        Map<Object,Object> ticketMeta = redis.opsForHash().entries(WaitingQueueKeys.ticketH(ticket.token()));
        assertThat(ticketMeta.get("state")).isEqualTo("working");
        Object mappedToken = redis.opsForHash().get(WaitingQueueKeys.activeTokenH(), String.valueOf(user.getId()));
        assertThat(mappedToken).isEqualTo(ticket.token());

        var paymentReq = new PaymentRequest(user.getId(), reservation.getId(), 1_000L);

        mockMvc.perform(
                post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Queue-Token", ticket.token()) // 보호 API: 토큰 헤더
                        .content(om.writeValueAsString(paymentReq))
        ).andExpect(result -> assertThat(result.getResponse().getStatus()).isBetween(200, 299));


        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo(String.valueOf(totalSeats - 1));
        });

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {

            Long r = redis.opsForZSet().rank(WaitingQueueKeys.workingZ(), ticket.token());
            assertThat(r).as("working zset must not contain token").isNull();


            String state = (String) redis.opsForHash().get(WaitingQueueKeys.ticketH(ticket.token()), "state");
            assertThat(state).isEqualTo("done");


            Object nowMapped = redis.opsForHash().get(WaitingQueueKeys.activeTokenH(), String.valueOf(user.getId()));
            assertThat(nowMapped).isNull();
        });
    }
}
