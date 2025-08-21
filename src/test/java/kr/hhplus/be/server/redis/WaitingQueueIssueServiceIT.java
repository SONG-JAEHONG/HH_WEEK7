package kr.hhplus.be.server.redis;


import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import org.junit.jupiter.api.*;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        // 스케줄러 자동 실행 방지 (직접 호출로 검증)
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class WaitingQueueIssueServiceIT {
    @Autowired
    private WaitingQueueIssueUseCase waitingQueueIssueUseCase;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanRedis() {

        RKeys keys = redisson.getKeys();

        keys.deleteByPattern("z:{queue}:waiting");
        keys.deleteByPattern("z:{queue}:working");
        keys.deleteByPattern("h:{queue}:active_token");
        keys.deleteByPattern("h:{queue}:ticket:*");
        keys.deleteByPattern("seq:{queue}:waiting:*");
    }

    @Test
    @DisplayName("신규 발급: 빈 대기열이면 rank=1, active_token/티켓 메타가 저장된다")
    void issue_newToken_rank1_and_metaStored() {
        long userId = 101L;

        IssueTicketResponse res = waitingQueueIssueUseCase.issueTicket(userId);

        assertNotNull(res);
        assertNotNull(res.token());
        assertEquals(1L, res.rank(), "첫 진입이면 순번은 1");


        Long rank = redisTemplate.opsForZSet().rank(WaitingQueueKeys.waitingZ(), res.token());
        assertNotNull(rank, "대기열에 방금 발급된 토큰이 있어야 함");


        Object mapped = redisTemplate.opsForHash()
                .get(WaitingQueueKeys.activeTokenH(), String.valueOf(userId));
        assertEquals(res.token(), mapped);


        Map<Object, Object> ticket = redisTemplate.opsForHash()
                .entries(WaitingQueueKeys.ticketH(res.token()));
        assertEquals(String.valueOf(userId), ticket.get("userId"));
        assertEquals("waiting", ticket.get("state"));
        assertNotNull(ticket.get("issuedAt"));
        assertNotNull(ticket.get("enqueuedAt"));
    }

    @Test
    @DisplayName("동시 발급: 최종 대기열의 순서는 1..N으로 유일하다")
    void issue_manyConcurrent_users_finalRanksUnique() throws Exception {
        int N = 100;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<IssueTicketResponse>> tasks = new ArrayList<>();
            IntStream.rangeClosed(1, N).forEach(i ->
                    tasks.add(() -> waitingQueueIssueUseCase.issueTicket((long) (10_000 + i)))
            );

            List<Future<IssueTicketResponse>> futures = pool.invokeAll(tasks);
            List<IssueTicketResponse> results = new ArrayList<>(N);
            for (Future<IssueTicketResponse> f : futures) {
                results.add(f.get(10, TimeUnit.SECONDS));
            }


            List<String> tokens = results.stream().map(IssueTicketResponse::token).toList();
            assertEquals(N, new HashSet<>(tokens).size(), "토큰은 모두 고유해야 함");


            Long zcard = redisTemplate.opsForZSet().zCard(WaitingQueueKeys.waitingZ());
            assertEquals(N, zcard);


            List<Long> finalRanks = new ArrayList<>(N);
            for (String tk : tokens) {
                Long r = redisTemplate.opsForZSet().rank(WaitingQueueKeys.waitingZ(), tk);
                assertNotNull(r, "토큰이 대기열에 있어야 함");
                finalRanks.add(r + 1); // 1-base로
            }

            assertEquals(N, new HashSet<>(finalRanks).size(), "최종 랭크는 유일해야 함");
            assertEquals(1L, finalRanks.stream().min(Long::compareTo).orElseThrow());
            assertEquals(N, finalRanks.stream().max(Long::compareTo).orElseThrow());

        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("여러 사용자 연속 발급: 순번은 1..N으로 증가한다")
    void issue_manySequential_users_rankIncrements() {
        List<IssueTicketResponse> results = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            results.add(waitingQueueIssueUseCase.issueTicket((long) (300 + i)));
        }


        for (int i = 0; i < results.size(); i++) {
            assertEquals(i + 1, results.get(i).rank(), "연속 발급이면 순번은 1부터 차례로 증가");
        }

        RScoredSortedSet<String> waiting = redisson.getScoredSortedSet(WaitingQueueKeys.waitingZ());
        assertEquals(5, waiting.size());
    }



}
