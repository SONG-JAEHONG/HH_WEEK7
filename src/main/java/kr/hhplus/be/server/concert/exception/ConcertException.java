package kr.hhplus.be.server.concert.exception;

import kr.hhplus.be.server.exception.DomainException;

public class ConcertException extends DomainException {

    private final ConcertErrorCode concertErrorCode;

    public ConcertException(ConcertErrorCode concertErrorCode, String message) {
        super(message);
        this.concertErrorCode = concertErrorCode;
    }

    public ConcertErrorCode getErrorCode(){
        return concertErrorCode;
    }

}
