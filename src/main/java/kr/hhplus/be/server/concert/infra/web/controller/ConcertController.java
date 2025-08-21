package kr.hhplus.be.server.concert.infra.web.controller;


import jakarta.validation.Valid;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.infra.web.dto.*;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import kr.hhplus.be.server.concert.port.in.ConcertUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertUseCase concertUseCase;
    private final ConcertCommandUseCase concertCommandUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateConcertResponse create(@RequestBody @Valid CreateConcertRequest req) {
        return concertCommandUseCase.createConcert(req);
    }

    @GetMapping
    public List<ConcertResponse> getAllConcerts(){
        return concertUseCase.getAllConcerts();
    }

    @GetMapping("/{concertId}/dates")
    public List<ConcertDateResponse> getConcertDates(@PathVariable("concertId") Long concertId){
        return concertUseCase.getConcertDates(concertId);
    }

    @GetMapping("/{concertDateId}/seats")
    public List<SeatResponse> getSeats(@PathVariable("concertDateId") Long concertDateId){
        return concertUseCase.getSeats(concertDateId);
    }

}
