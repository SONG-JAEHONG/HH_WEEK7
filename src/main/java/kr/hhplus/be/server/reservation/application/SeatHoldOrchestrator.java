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
        String key = "lock:seat:" + seatId;

        Duration wait = Duration.ofMillis(0);
        Duration lease = Duration.ofMillis(2000);

        return lockManager.lock(
                key,
                wait,
                lease,
                () -> seatHoldService.holdSeat(seatId)
        );
    }
}