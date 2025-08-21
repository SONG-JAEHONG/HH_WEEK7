package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.common.lock.RedisLockManager;
import kr.hhplus.be.server.concert.domain.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SeatHoldOrchestrator {
    private final RedisLockManager lockManager;
    private final SeatHoldService seatHoldService;

    public Seat holdSeatWithLock(Long seatId) {
        String lockKey = "lock:seat:" + seatId;
        String tokenKey = "seq:fence:seat:" + seatId;

        Duration wait = Duration.ofMillis(0);
        Duration lease = Duration.ofMillis(2000);

        return lockManager.lockWithFencingToken(
                lockKey, tokenKey, wait, lease,
                token -> seatHoldService.holdSeat(seatId, token)
        );
    }
}