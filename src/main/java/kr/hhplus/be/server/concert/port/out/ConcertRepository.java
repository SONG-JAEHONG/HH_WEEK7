package kr.hhplus.be.server.concert.port.out;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.exception.ConcertException;
import kr.hhplus.be.server.user.exception.UserErrorCode;
import kr.hhplus.be.server.user.exception.UserException;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConcertRepository {

    List<Concert> findAllConcerts();
    List<ConcertDate> findConcertDatesByConcertId(Long concertId);
    List<Seat> findAvailableSeatsByConcertDateId(Long concertDateId, SeatStatus status);
    Optional<ConcertDate> findConcertDateById(Long concertDateId);
    Concert save(Concert concert);
    ConcertDate save(ConcertDate concertDate);

    default ConcertDate findConcertDateByIdOrThrow(Long concertDateId){
        return findConcertDateById(concertDateId).orElseThrow(() -> new ConcertException(
                ConcertErrorCode.CONCERT_DATE_NOT_FOUND, "존재하지 않는 콘서트 날짜입니다. concertDateId=" + concertDateId));
    }

    int updateSellOut(Long dateId, LocalDateTime now, long seconds);



}