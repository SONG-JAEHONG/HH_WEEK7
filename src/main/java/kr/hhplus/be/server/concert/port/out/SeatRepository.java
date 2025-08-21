package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.exception.ConcertException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    Optional<Seat> findSeatById(Long id);

    default Seat findSeatByIdOrThrow(Long id){
        return findSeatById(id).orElseThrow(() -> new ConcertException(ConcertErrorCode.SEAT_NOT_FOUND,"존재하지 않는 좌석입니다. seatId = " + id));
    }

    void save(Seat seat);

    List<Seat> findByStatusAndBeforeExpire(SeatStatus status, LocalDateTime time);

    Optional<Seat> findSeatByIdWithLock(Long id);

    default Seat findSeatByIdWithLockOrThrow(Long id) {
        return findSeatByIdWithLock(id)
                .orElseThrow(() -> new ConcertException(
                        ConcertErrorCode.SEAT_NOT_FOUND,
                        "존재하지 않는 좌석입니다. seatId = " + id
                ));
    }

    int tryHoldWithToken(Long seatId, LocalDateTime expiresAt, LocalDateTime now, long token, SeatStatus available , SeatStatus holding);

}
