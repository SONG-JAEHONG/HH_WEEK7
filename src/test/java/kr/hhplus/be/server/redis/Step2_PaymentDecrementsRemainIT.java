package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.concert.infra.redis.RedisRankKeys;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import kr.hhplus.be.server.concert.infra.persistence.ConcertDateJpaRepository;
import kr.hhplus.be.server.concert.infra.persistence.ConcertJpaRepository;
import kr.hhplus.be.server.concert.infra.persistence.SeatJpaRepository;
import kr.hhplus.be.server.concert.domain.*;
import kr.hhplus.be.server.payment.infra.persistence.PaymentJpaRepository;
import kr.hhplus.be.server.payment.infra.web.dto.PaymentRequest;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.domain.ReservationStatus;
import kr.hhplus.be.server.reservation.infra.persistence.ReservationJpaRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infra.persistence.UserJpaRepository;
import kr.hhplus.be.server.concert.infra.redis.RedisKeys;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Step2_PaymentDecrementsRemainIT {

    @Autowired MockMvc mockMvc;

    @Autowired ConcertCommandUseCase concertCommandUseCase;
    @Autowired ConcertJpaRepository concertRepo;
    @Autowired ConcertDateJpaRepository concertDateRepo;
    @Autowired SeatJpaRepository seatRepo;
    @Autowired
    PaymentJpaRepository paymentJpaRepository;
    @Autowired UserJpaRepository userRepo;
    @Autowired ReservationJpaRepository reservationRepo;

    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        paymentJpaRepository.deleteAllInBatch();
        reservationRepo.deleteAllInBatch();
        seatRepo.deleteAllInBatch();
        concertDateRepo.deleteAllInBatch();
        concertRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    @Test
    void 결제_성공_시_남은좌석_1감소() throws Exception {

        int totalSeats = 150;
        LocalDate date = LocalDate.of(2025, 9, 1);
        LocalDateTime openAt = LocalDateTime.of(2025, 8, 20, 9, 0, 0);

        var req = new CreateConcertRequest(
                "테스트콘서트",
                List.of(new CreateConcertRequest.Date(date, openAt, totalSeats))
        );
        var createRes = concertCommandUseCase.createConcert(req);
        Long concertDateId = createRes.concertDateIds().get(0);

        var concertDate = concertDateRepo.findById(concertDateId).orElseThrow();
        var concert = concertDate.getConcert();

        String totalKey = RedisKeys.TotalSeat(concertDateId);
        String remainKey = RedisKeys.RemainSeat(concertDateId);


        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(totalKey)).isEqualTo("150");
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo("150");
        });


        Seat seat = seatRepo.save(
                Seat.builder()
                        .concertDate(concertDate)
                        .number(1)
                        .status(SeatStatus.HOLDING)
                        .build()
        );


        User user = userRepo.save(User.builder().point(5000L).build());

        Reservation reservation = reservationRepo.save(
                Reservation.builder()
                        .user(user)
                        .concertDate(concertDate)
                        .seat(seat)
                        .status(ReservationStatus.HOLDING)
                        .build()
        );


        var paymentReq = new PaymentRequest(user.getId(), reservation.getId(), 1000L);

        mockMvc.perform(
                post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(paymentReq))
        ).andExpect(result -> assertThat(result.getResponse().getStatus()).isBetween(200, 299));


        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(redis.opsForValue().get(remainKey)).isEqualTo("149");
        });

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redis.opsForZSet().rangeWithScores(RedisRankKeys.ratioAll(), 0, -1);

            assertThat(tuples).isNotNull().hasSize(1);

            ZSetOperations.TypedTuple<String> only = tuples.iterator().next();


            String expectedMember = RedisRankKeys.member(concert.getId(), concertDateId);
            assertThat(only.getValue()).isEqualTo(expectedMember);


            double expectedScore = 1.0 / totalSeats;
            assertThat(only.getScore()).isNotNull();
            assertThat(only.getScore()).isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(1e-9));
        });

    }

}
