package kr.hhplus.be.server.reservation.infra.web.dto;

public record ReservationRequest(Long userId ,Long concertDateId, Long seatId){
}
