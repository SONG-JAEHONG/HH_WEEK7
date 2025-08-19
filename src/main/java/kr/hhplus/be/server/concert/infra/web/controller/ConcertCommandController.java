package kr.hhplus.be.server.concert.infra.web.controller;


import jakarta.validation.Valid;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertRequest;
import kr.hhplus.be.server.concert.infra.web.dto.CreateConcertResponse;
import kr.hhplus.be.server.concert.port.in.ConcertCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/concerts/command")
@RequiredArgsConstructor
public class ConcertCommandController {

    private final ConcertCommandUseCase concertCommandUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateConcertResponse createConcert(@RequestBody @Valid CreateConcertRequest request) {
        return concertCommandUseCase.createConcert(request);
    }

}
