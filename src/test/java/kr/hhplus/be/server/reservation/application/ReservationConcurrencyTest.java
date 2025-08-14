package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.common.lock.RedisLockManager;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.infra.persistence.SeatJpaRepository;
import kr.hhplus.be.server.concert.infra.persistence.ConcertDateJpaRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.infra.persistence.ReservationJpaRepository;
import kr.hhplus.be.server.reservation.infra.web.dto.ReservationRequest;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.infra.persistence.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
public class ReservationConcurrencyTest  {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test-db")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RedisLockManager redisLockManager;
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    private Long seatId;
    private Long concertDateId;
    private List<Long> userIds;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();

        Seat seat = new Seat(null, 10, SeatStatus.AVAILABLE, null);
        seatJpaRepository.save(seat);
        seatId = seat.getId();

        ConcertDate concertDate = new ConcertDate(null, null, LocalDate.now().plusDays(1));
        concertDateJpaRepository.save(concertDate);
        concertDateId = concertDate.getId();

        userIds = List.of(
                userJpaRepository.save(new User(null, "user1", 10000L)).getId(),
                userJpaRepository.save(new User(null, "user2", 10000L)).getId(),
                userJpaRepository.save(new User(null, "user3", 10000L)).getId()
        );
    }

    @Test
    void 동시에_여러명이_동일한_좌석을_예약하면_1명만_성공하고_락_시간_측정() throws InterruptedException {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> lockTimes = new ConcurrentLinkedQueue<>();

        for (Long userId : userIds) {
            executor.submit(() -> {
                try {
                    long startNs = System.nanoTime();
                    redisLockManager.lock("seat:" + seatId,
                            Duration.ofSeconds(0),
                            Duration.ofSeconds(5),
                            () -> {
                                try {
                                    reservationService.reserve(
                                            new ReservationRequest(userId, concertDateId, seatId),
                                            userId
                                    );
                                    results.add("SUCCESS");
                                } catch (Exception e) {
                                    results.add("FAIL-" + e.getClass().getSimpleName());
                                }
                                return null;
                            });
                    long endNs = System.nanoTime();
                    lockTimes.add(endNs - startNs);
                } catch (Exception e) {
                    results.add("FAIL-" + e.getClass().getSimpleName());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long successCount = results.stream().filter(r -> r.equals("SUCCESS")).count();
        long failCount = results.stream().filter(r -> r.startsWith("FAIL")).count();

        assertEquals(1, successCount);
        assertEquals(2, failCount);

        List<Reservation> allReservations = reservationJpaRepository.findAll();
        assertThat(allReservations).hasSize(1);
        assertThat(allReservations.get(0).getSeat().getId()).isEqualTo(seatId);


        lockTimes.forEach(time -> System.out.println("Lock time (ms): " + time / 1_000_000));
    }
}
