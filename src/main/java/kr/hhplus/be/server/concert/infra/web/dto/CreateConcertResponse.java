package kr.hhplus.be.server.concert.infra.web.dto;

import java.util.List;

public record CreateConcertResponse(
        Long concertId,
        List<Long> concertDateIds
) {}