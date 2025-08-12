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
        String key = "lock:seat:" + seatId; // 좌석별 키
        return lockManager.lock(
                key,
                Duration.ofMillis(200),
                Duration.ofSeconds(3),
                () -> seatHoldService.holdSeat(seatId)
        );
    }
}