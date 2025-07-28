package kr.hhplus.be.server.reservation.infra.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationResponse {
    private Long reservationId;
    private Long seatId;
    private String status;
}
