package kr.hhplus.be.server.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.concert.domain.*;
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
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import kr.hhplus.be.server.waiting.port.in.WaitingQueuePromoteUseCase;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "queue.promote.capacity=10",
        "queue.promote.max-batch=1000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentOversellAndIdempotencyIT {

    @Autowired MockMvc mockMvc;
    @Autowired StringRedisTemplate redis;
    @Autowired ConcertCommandUseCase concertCmd;
    @Autowired ConcertJpaRepository concertRepo;
    @Autowired ConcertDateJpaRepository dateRepo;
    @Autowired SeatJpaRepository seatRepo;
    @Autowired ReservationJpaRepository resRepo;
    @Autowired PaymentJpaRepository payRepo;
    @Autowired UserJpaRepository userRepo;

    @Autowired WaitingQueueIssueUseCase issue;
    @Autowired WaitingQueuePromoteUseCase promote;

    private static final ObjectMapper om = new ObjectMapper();

    @AfterEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        payRepo.deleteAllInBatch();
        resRepo.deleteAllInBatch();
        seatRepo.deleteAllInBatch();
        dateRepo.deleteAllInBatch();
        concertRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("오버셀=0: 동일 예약에 동시 결제 50건 → 성공 1, remainSeat 정확히 1 감소")
    void noOversell_sameReservation_manyConcurrentPayments() throws Exception {

        int totalSeats = 5;
        var create = concertCmd.createConcert(new CreateConcertRequest(
                "동시결제테스트",
                List.of(new CreateConcertRequest.Date(LocalDate.now().plusDays(10),
                        LocalDateTime.now().plusDays(1), totalSeats))
        ));
        Long dateId = create.concertDateIds().get(0);
        var concertDate = dateRepo.findById(dateId).orElseThrow();

        String remainKey = RedisKeys.RemainSeat(dateId);
        String totalKey  = RedisKeys.TotalSeat(dateId);
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(totalKey)).isEqualTo(String.valueOf(totalSeats));
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo(String.valueOf(totalSeats));
        });

        Seat seat = seatRepo.save(Seat.builder().concertDate(concertDate).number(1).status(SeatStatus.HOLDING).build());
        User user = userRepo.save(User.builder().point(100_000L).build());
        Reservation reservation = resRepo.save(Reservation.builder()
                .user(user).concertDate(concertDate).seat(seat).status(ReservationStatus.HOLDING).build());


        String token = issue.issueTicket(user.getId()).token();
        promote.promoteOnce();
        assertThat(redis.opsForZSet().rank(WaitingQueueKeys.workingZ(), token)).isNotNull();


        int N = 50;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            calls.add(() -> {
                start.await();
                var req = new PaymentRequest(user.getId(), reservation.getId(), 1000L);
                var res = mockMvc.perform(post("/payments")
                        .header("X-Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req))
                ).andReturn().getResponse();
                return res.getStatus();
            });
        }
        start.countDown();

        int ok = 0;
        for (Future<Integer> f : pool.invokeAll(calls)) {
            int status = f.get(10, TimeUnit.SECONDS);
            if (status >= 200 && status < 300) ok++;
        }
        pool.shutdownNow();

        assertThat(ok).isEqualTo(1);

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(redis.opsForValue().get(remainKey)).isEqualTo(String.valueOf(totalSeats - 1)));


        assertThat(payRepo.count()).isLessThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("결제 엔드포인트 멱등성: 동일 요청 2회 → 첫 성공, 두번째는 실패/무시, out은 1회만")
    void paymentEndpoint_isIdempotent() throws Exception {

        var create = concertCmd.createConcert(new CreateConcertRequest(
                "멱등결제", List.of(new CreateConcertRequest.Date(LocalDate.now().plusDays(3),
                LocalDateTime.now().plusHours(2), 10))
        ));
        Long dateId = create.concertDateIds().get(0);
        var concertDate = dateRepo.findById(dateId).orElseThrow();
        String remainKey = RedisKeys.RemainSeat(dateId);

        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(redis.opsForValue().get(remainKey)).isEqualTo("10"));

        Seat seat = seatRepo.save(Seat.builder().concertDate(concertDate).number(2).status(SeatStatus.HOLDING).build());
        User user = userRepo.save(User.builder().point(10_000L).build());
        Reservation reservation = resRepo.save(Reservation.builder()
                .user(user).concertDate(concertDate).seat(seat).status(ReservationStatus.HOLDING).build());

        String token = issue.issueTicket(user.getId()).token();
        promote.promoteOnce();

        var req = new PaymentRequest(user.getId(), reservation.getId(), 1000L);
        var r1 = mockMvc.perform(post("/payments")
                        .header("X-Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andReturn().getResponse();
        assertThat(r1.getStatus()).isBetween(200, 299);

        var r2 = mockMvc.perform(post("/payments")
                        .header("X-Queue-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andReturn().getResponse();
        assertThat(r2.getStatus() / 100).isNotEqualTo(2);
        assertThat(r2.getStatus()).isGreaterThanOrEqualTo(300);


        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForZSet().rank(WaitingQueueKeys.workingZ(), token)).isNull();
            assertThat(redis.opsForHash().get(WaitingQueueKeys.activeTokenH(), String.valueOf(user.getId()))).isNull();
            assertThat(redis.opsForHash().get(WaitingQueueKeys.ticketH(token), "state")).isEqualTo("done");
        });

        assertThat(redis.opsForValue().get(remainKey)).isEqualTo("9");
    }
}
