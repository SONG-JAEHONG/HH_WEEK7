package kr.hhplus.be.server.payment.infra.web.dto;

public record PaymentRequest( Long userId, Long reservationId, Long amount) { }
