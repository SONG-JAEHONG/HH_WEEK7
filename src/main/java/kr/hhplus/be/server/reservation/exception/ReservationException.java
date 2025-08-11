package kr.hhplus.be.server.reservation.exception;

import kr.hhplus.be.server.exception.DomainException;

import java.nio.file.attribute.FileAttribute;

public class ReservationException extends DomainException {

    private final ReservationErrorCode reservationErrorCode;

    public ReservationException(ReservationErrorCode reservationErrorCode, String message) {
        super(message);
        this.reservationErrorCode = reservationErrorCode;
    }

    public ReservationErrorCode getErrorCode() {
        return  reservationErrorCode;
    }
}
