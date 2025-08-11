package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.Seat;
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
    public Seat holdSeat(Long seatId) {
        Seat seat = seatRepository.findSeatByIdOrThrow(seatId);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(5);
        seat.hold(expiresAt);
        seatRepository.save(seat);
        return seat;
    }
}