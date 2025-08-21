package kr.hhplus.be.server.concert.infra.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreateConcertRequest(
        @NotBlank String title,
        @NotNull List<Date> dates
) {
    public record Date(
            @NotNull LocalDate concertDate,
            @NotNull LocalDateTime openAt,
            @Min(1) int totalSeats
    ) {}
}