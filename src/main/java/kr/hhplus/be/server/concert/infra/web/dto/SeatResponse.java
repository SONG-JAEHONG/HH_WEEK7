package kr.hhplus.be.server.concert.infra.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private int seatNumber;

}

