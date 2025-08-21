package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.port.out.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final Clock clock;

    @Transactional
    public Seat holdSeat(Long seatId, long token) {
        Seat seat = seatRepository.findSeatByIdOrThrow(seatId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusMinutes(5);
        int changed = seatRepository.tryHoldWithToken(seatId, expiresAt, now, token, SeatStatus.AVAILABLE , SeatStatus.HOLDING);
        if (changed != 1) {
            throw new IllegalStateException("좌석 선점 실패(경합/상태/토큰) seatId=" + seatId);
        }
        seat.hold(expiresAt);
        seatRepository.save(seat);
        return seat;
    }
}