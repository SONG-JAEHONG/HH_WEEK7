package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.port.out.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;

    @Transactional
    public Seat holdSeat(Long seatId) {
        Seat seat = seatRepository.findSeatById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        if (!seat.isAvailable()) {
            throw new IllegalStateException("이미 예약 중인 좌석입니다.");
        }

        seat.hold();       // 상태 변경
        seatRepository.save(seat); // 업데이트 반영

        return seat;
    }
}