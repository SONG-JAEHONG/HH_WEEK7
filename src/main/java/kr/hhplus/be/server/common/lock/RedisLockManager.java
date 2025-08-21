package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RedisLockManager {
    private final RedissonClient redissonClient;

    public <T> T lock(String key, Duration wait, Duration lease, Supplier<T> body) {

        RLock lock = redissonClient.getLock(key);
        boolean getLock = false;

        try {
            getLock = lock.tryLock(wait.toMillis(), lease.toMillis(), TimeUnit.MILLISECONDS);
            System.out.println("[LOCK] " + key + " acquired=" + getLock + " thread=" + Thread.currentThread().getName());
            if (!getLock) {
                throw new LockException("락 획득 실패: " + key);
            }
            return body.get();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new LockException("락 인터럽트: " + key, e);

        } finally {

            try {

                if (getLock && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }

            } catch (IllegalMonitorStateException ignore) {}
        }
    }

    public <T> T lockWithFencingToken(
            String lockKey,
            String tokenSeqKey,
            Duration wait,
            Duration lease,
            Function<Long, T> body
    ) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean getLock = false;
        try {
            getLock = lock.tryLock(wait.toMillis(), lease.toMillis(), TimeUnit.MILLISECONDS);
            if (!getLock) throw new LockException("락 획득 실패: " + lockKey);

            RAtomicLong seq = redissonClient.getAtomicLong(tokenSeqKey);
            long token = seq.incrementAndGet();

            return body.apply(token);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockException("락 인터럽트: " + lockKey, e);
        } finally {
            try { if (getLock && lock.isHeldByCurrentThread()) lock.unlock(); } catch (IllegalMonitorStateException ignore) {}
        }
    }
}