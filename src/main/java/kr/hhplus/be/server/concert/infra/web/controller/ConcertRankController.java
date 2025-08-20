package kr.hhplus.be.server.concert.infra.web.controller;


import kr.hhplus.be.server.concert.port.in.GetRankUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts/rank")
public class ConcertRankController {

    private final GetRankUseCase getRankUseCase;

    @GetMapping
    public GetRankUseCase.Result top(@RequestParam(defaultValue = "20") int limit) {
        return getRankUseCase.top(Math.max(1, Math.min(100, limit)));
    }

}
