package kr.hhplus.be.server.concert.infra.web.controller;

import jakarta.validation.constraints.Pattern;
import kr.hhplus.be.server.concert.port.in.GetDailySelloutRankUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts/sellout/daily")
@Validated
public class SelloutDailyRankQueryController {

    private final GetDailySelloutRankUseCase getDailySelloutRankUseCase;

    @GetMapping
    public GetDailySelloutRankUseCase.Result get(
            @RequestParam
            @Pattern(regexp = "^\\d{8}$", message = "yyyyMMdd")
            String date
    ) {
        return getDailySelloutRankUseCase.get(date);
    }
}