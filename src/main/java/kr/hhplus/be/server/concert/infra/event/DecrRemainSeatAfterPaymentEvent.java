package kr.hhplus.be.server.concert.infra.event;

public record DecrRemainSeatAfterPaymentEvent(long reservationId, long concertDateId) {
}
