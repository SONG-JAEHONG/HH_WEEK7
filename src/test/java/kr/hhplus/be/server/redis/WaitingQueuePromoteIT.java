package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.infra.web.dto.IssueTicketResponse;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueIssueUseCase;
import kr.hhplus.be.server.waiting.port.in.WaitingQueuePromoteUseCase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "queue.promote.capacity=10",
        "queue.promote.max-batch=1000",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class WaitingQueuePromoteIT {

    @Autowired private WaitingQueueIssueUseCase issueUseCase;
    @Autowired private WaitingQueuePromoteUseCase promoteUseCase;
    @Autowired private StringRedisTemplate redis;
    @Autowired private kr.hhplus.be.server.waiting.config.PromoteProps props;

    private String W() { return WaitingQueueKeys.waitingZ(); }
    private String K() { return WaitingQueueKeys.workingZ(); }

    @BeforeEach
    void cleanRedis() {
        // 관련 키 싹 정리
        redis.delete(W());
        redis.delete(K());
        redis.delete(WaitingQueueKeys.activeTokenH());
        var t = redis.keys("h:{queue}:ticket:*"); if (t != null && !t.isEmpty()) redis.delete(t);
        var s = redis.keys("seq:{queue}:waiting:*"); if (s != null && !s.isEmpty()) redis.delete(s);


        assertEquals(10, props.getCapacity(), "capacity mismatch");
        assertTrue(props.getMaxBatch() >= 1000, "maxBatch too small for tests");
    }


    private long zcard(String key) { return Optional.ofNullable(redis.opsForZSet().zCard(key)).orElse(0L); }
    private long freeSlots()       { return props.getCapacity() - zcard(K()); }
    private void enqueueUsers(long startUserId, int n) {
        for (int i = 0; i < n; i++) issueUseCase.issueTicket(startUserId + i);
    }
    private Set<String> workingMembers(int n) {
        var r = redis.opsForZSet().range(K(), 0, n - 1);
        return r == null ? Set.of() : r;
    }
    private void releaseFromWorking(int n) {
        var members = workingMembers(n);
        if (!members.isEmpty()) redis.opsForZSet().remove(K(), members.toArray());
        for (String tk : members) {
            redis.opsForHash().put(WaitingQueueKeys.ticketH(tk), "state", "done");
        }
    }

    private long expectedMovedNow() {
        long fs = freeSlots();
        long waitingSize = zcard(W());
        return min(min(fs, waitingSize), props.getMaxBatch());
    }

    @Test
    @DisplayName("승격: freeSlots가 waiting 이상이면 모두 working으로 이동한다")
    void promote_all_when_freeSlots_ge_waiting() {
        assertEquals(0L, zcard(K()), "working must be empty");
        int waiting = 5;
        enqueueUsers(10_000, waiting);

        long expectedMoved = expectedMovedNow();
        var r = promoteUseCase.promoteOnce();

        assertEquals(expectedMoved, r.moved(), "moved should equal min(free, waiting, maxBatch)");
        assertEquals(0L, zcard(W()), "waiting should be empty after promote");
        assertEquals(waiting, zcard(K()), "working should have all");
        for (String tk : r.tokens()) {
            var st = redis.opsForHash().get(WaitingQueueKeys.ticketH(tk), "state");
            assertEquals("working", st);
        }
    }

    @Test
    @DisplayName("승격: freeSlots가 waiting보다 작으면 freeSlots만큼만 승격된다")
    void promote_only_freeSlots_when_waiting_exceeds_freeSlots() {

        enqueueUsers(20_000, 7);
        promoteUseCase.promoteOnce();
        assertEquals(7L, zcard(K()), "working should be 7");


        enqueueUsers(30_000, 10);
        long expected = expectedMovedNow(); // 보통 3

        var r = promoteUseCase.promoteOnce();
        assertEquals(expected, r.moved(), "should move only freeSlots");
        assertEquals(7 + expected, zcard(K()), "working should be 10");
        assertEquals(10 - expected, zcard(W()), "waiting should have the rest");
    }

    @Test
    @DisplayName("승격: working이 꽉 차면 이동 0 → 일부가 비면 다시 그만큼 채운다")
    void promote_when_full_then_refill_after_release() {

        enqueueUsers(40_000, props.getCapacity());
        promoteUseCase.promoteOnce();
        assertEquals(props.getCapacity(), zcard(K()));
        assertEquals(0L, zcard(W()));


        var r0 = promoteUseCase.promoteOnce();
        assertEquals(0, r0.moved());


        releaseFromWorking(4);
        assertEquals(props.getCapacity() - 4, zcard(K()));


        enqueueUsers(50_000, 10);
        assertEquals(10L, zcard(W()));

        long expected = expectedMovedNow();
        var r1 = promoteUseCase.promoteOnce();
        assertEquals(expected, r1.moved());
        assertEquals(props.getCapacity(), zcard(K()), "working refilled to capacity");
        assertEquals(10 - expected, zcard(W()));
    }

}
