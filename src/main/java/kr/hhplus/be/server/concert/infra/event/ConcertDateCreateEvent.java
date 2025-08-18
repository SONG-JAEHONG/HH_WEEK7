package kr.hhplus.be.server.concert.infra.event;

public record ConcertDateCreateEvent(long concertDateId, int totalSeats) {
}
