package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "queue.promote.capacity=10",
        "queue.promote.max-batch=1000"
})
@ActiveProfiles("test")
class QueueTokenConcurrencyIT {

    @Autowired WaitingQueueIssueUseCase issueUseCase;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void clean() {
        redis.delete(WaitingQueueKeys.waitingZ());
        redis.delete(WaitingQueueKeys.workingZ());
        redis.delete(WaitingQueueKeys.activeTokenH());
        var t = redis.keys("h:{queue}:ticket:*"); if (t != null) redis.delete(t);
        var s = redis.keys("seq:{queue}:waiting:*"); if (s != null) redis.delete(s);
    }

    @Test
    @DisplayName("동일 userId 동시 발급 → 항상 같은 토큰(중복토큰=0)")
    void sameUserConcurrent_issueSameToken() throws Exception {
        long userId = 777L;
        int N = 100;

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            fs.add(pool.submit(() -> { start.await(); return issueUseCase.issueTicket(userId).token(); }));
        }
        start.countDown();

        Set<String> tokens = new HashSet<>();
        for (Future<String> f : fs) tokens.add(f.get(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, tokens.size(), "같은 유저는 어떤 동시성 하에서도 토큰 하나만");

        assertEquals(tokens.iterator().next(), redis.opsForHash()
                .get(WaitingQueueKeys.activeTokenH(), String.valueOf(userId)));
    }

    @Test
    @DisplayName("다수 user 동시 발급 → 토큰 모두 고유, waiting 크기 = N")
    void manyUsersConcurrent_uniqueTokens() throws Exception {
        int N = 300;

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        List<Future<String>> fs = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            long uid = 10_000L + i;
            fs.add(pool.submit(() -> { start.await(); return issueUseCase.issueTicket(uid).token(); }));
        }
        start.countDown();

        Set<String> tokens = new HashSet<>();
        for (Future<String> f : fs) tokens.add(f.get(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(N, tokens.size(), "서로 다른 유저는 각자 고유 토큰");
        Long waitingSize = redis.opsForZSet().zCard(WaitingQueueKeys.waitingZ());
        Long workingSize = redis.opsForZSet().zCard(WaitingQueueKeys.workingZ());
        System.out.println("waiting=" + waitingSize + ", working=" + workingSize);
        assertEquals(N, waitingSize + workingSize, "waiting+working == N 이어야 함");
    }
}
