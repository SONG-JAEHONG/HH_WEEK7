package kr.hhplus.be.server.concert.application;


import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.infra.web.dto.ConcertDateResponse;
import kr.hhplus.be.server.concert.infra.web.dto.ConcertResponse;
import kr.hhplus.be.server.concert.infra.web.dto.SeatResponse;
import kr.hhplus.be.server.concert.port.in.ConcertUseCase;

import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ConcertService implements ConcertUseCase {

    private final ConcertRepository concertRepository;

    @Cacheable(cacheNames = "concert:list", key = "'all'")
    @Transactional(readOnly = true)
    @Override
    public List<ConcertResponse> getAllConcerts(){
        return concertRepository.findAllConcerts().stream()
                .map(concert -> new ConcertResponse(concert.getId(), concert.getTitle()))
                .toList();
    }

    @Cacheable(cacheNames = "concert:dates", key = "#concertId")
    @Transactional(readOnly = true)
    @Override
    public List<ConcertDateResponse> getConcertDates(Long concertId) {
        return concertRepository.findConcertDatesByConcertId(concertId).stream()
                .map(concertDate -> new ConcertDateResponse(concertDate.getId(),concertDate.getConcertDate()))
                .toList();
    }

    @Override
    public List<SeatResponse> getSeats(Long concertDateId) {
        return concertRepository.findAvailableSeatsByConcertDateId(concertDateId, SeatStatus.AVAILABLE).stream()
                .map(seat -> new SeatResponse(seat.getId(), seat.getSeatNumber()))
                .toList();
    }


}
