package kr.hhplus.be.server.exception;

import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.exception.ConcertException;
import kr.hhplus.be.server.reservation.exception.ReservationException;
import kr.hhplus.be.server.user.exception.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getErrorCode().name(), e.getMessage()));
    }
    @ExceptionHandler(ConcertException.class)
    public ResponseEntity<ErrorResponse> handleConcertException(ConcertException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getErrorCode().name(), e.getMessage()));
    }
    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handleReservationException(ReservationException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getErrorCode().name(), e.getMessage()));
    }
}
