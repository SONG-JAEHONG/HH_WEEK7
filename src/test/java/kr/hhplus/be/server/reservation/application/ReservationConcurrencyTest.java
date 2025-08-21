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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class ReservationConcurrencyTest {

    @Autowired ReservationService reservationService;
    @Autowired RedisLockManager redisLockManager;
    @Autowired SeatJpaRepository seatJpaRepository;
    @Autowired UserJpaRepository userJpaRepository;
    @Autowired ConcertDateJpaRepository concertDateJpaRepository;
    @Autowired ReservationJpaRepository reservationJpaRepository;
    @Autowired StringRedisTemplate stringRedisTemplate;
    @Autowired PlatformTransactionManager txManager;

    private <T> T tx(java.util.function.Supplier<T> supplier) {
        return new TransactionTemplate(txManager).execute(status -> supplier.get());
    }
    private void txRun(Runnable r) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> r.run());
    }

    Long seatId;
    Long concertDateId;
    List<Long> userIds;

    @BeforeEach
    void setUp() {
        reservationJpaRepository.deleteAll();
        seatJpaRepository.deleteAll();
        concertDateJpaRepository.deleteAll();
        userJpaRepository.deleteAll();


        Seat seat = new Seat(null, 10, SeatStatus.AVAILABLE, null);
        seatJpaRepository.saveAndFlush(seat);
        seatId = seat.getId();

        ConcertDate cd = new ConcertDate(null, null, LocalDate.now().plusDays(1));
        concertDateJpaRepository.saveAndFlush(cd);
        concertDateId = cd.getId();

        userIds = Arrays.asList(
                userJpaRepository.saveAndFlush(new User(null, "user1", 10000L)).getId(),
                userJpaRepository.saveAndFlush(new User(null, "user2", 10000L)).getId(),
                userJpaRepository.saveAndFlush(new User(null, "user3", 10000L)).getId()
        );


        stringRedisTemplate.delete(tokenKey(seatId));
    }

    private String tokenKey(Long seatId) {
        return "seq:fence:seat:" + seatId;
    }


    @DisplayName("분산락: 3명 동시 → 1명만 성공(락으로 상호배제), 나머지는 실패 ")
    @Test
    void 분산락_동시_1명만_성공() throws InterruptedException {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> lockTimes = new ConcurrentLinkedQueue<>();

        for (Long userId : userIds) {
            executor.submit(() -> {
                try {
                    long startNs = System.nanoTime();
                    redisLockManager.lock("lock:seat:" + seatId,
                            Duration.ofMillis(0),
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

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        long success = results.stream().filter("SUCCESS"::equals).count();
        long fail = results.stream().filter(s -> s.startsWith("FAIL")).count();

        assertEquals(1, success);
        assertEquals(2, fail);

        List<Reservation> all = reservationJpaRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getSeat().getId()).isEqualTo(seatId);


        System.out.println("Lock times(ms): " + lockTimes.stream()
                .map(ns -> String.valueOf(ns / 1_000_000))
                .collect(Collectors.joining(", ")));
    }
    @DisplayName("페싱 토큰: 락 없이도 '큰 토큰'만 성공, 늦게 도착한 작은 토큰은 DB가 거절(영향행 0) ")
    @Test
    void 페싱토큰_락없이_늦은작업_거절() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<Integer> t1 = () -> {
            start.await();
            long t1Token = stringRedisTemplate.opsForValue().increment(tokenKey(seatId));
            Thread.sleep(150);


            return new TransactionTemplate(txManager).execute(status ->
                    seatJpaRepository.tryHoldWithToken(
                            seatId,
                            LocalDateTime.now().plusMinutes(5),
                            LocalDateTime.now(),
                            t1Token,
                            SeatStatus.AVAILABLE,
                            SeatStatus.HOLDING
                    )
            );
        };

        Callable<Integer> t2 = () -> {
            start.await();
            long t2Token = stringRedisTemplate.opsForValue().increment(tokenKey(seatId));
            return new TransactionTemplate(txManager).execute(status ->
                    seatJpaRepository.tryHoldWithToken(
                            seatId,
                            LocalDateTime.now().plusMinutes(5),
                            LocalDateTime.now(),
                            t2Token,
                            SeatStatus.AVAILABLE,
                            SeatStatus.HOLDING
                    )
            );
        };

        Future<Integer> f1 = pool.submit(t1);
        Future<Integer> f2 = pool.submit(t2);
        start.countDown();

        int c1 = f1.get(5, TimeUnit.SECONDS);
        int c2 = f2.get(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue((c1 == 1 && c2 == 0) || (c1 == 0 && c2 == 1));
        Seat after = seatJpaRepository.findById(seatId).orElseThrow();
        assertEquals(SeatStatus.HOLDING, after.getStatus());
    }

    @DisplayName("낙관적 락: 두 트랜잭션이 같은 좌석 수정을 커밋하면 두 번째에서 OptimisticLockingFailureException")
    @Test
    void 낙관적락_두트랜잭션_경합시_두번째_실패() {

        Seat stale = tx(() -> seatJpaRepository.findByIdWithOptimisticLock(seatId).orElseThrow());


        txRun(() -> {
            Seat fresh = seatJpaRepository.findByIdWithOptimisticLock(seatId).orElseThrow();
            fresh.hold(LocalDateTime.now().plusMinutes(10));
            seatJpaRepository.saveAndFlush(fresh);
        });


        assertThrows(OptimisticLockingFailureException.class, () -> {
            txRun(() -> seatJpaRepository.saveAndFlush(stale));
        });
    }
}
