package kr.hhplus.be.server.concert.port.in;


import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.infra.web.dto.ConcertDateResponse;
import kr.hhplus.be.server.concert.infra.web.dto.ConcertResponse;
import kr.hhplus.be.server.concert.infra.web.dto.SeatResponse;


import java.util.List;

public interface ConcertUseCase {


    List<ConcertResponse> getAllConcerts();
    List<ConcertDateResponse> getConcertDates(Long concertId);
    List<SeatResponse> getSeats(Long concertDateId);


}