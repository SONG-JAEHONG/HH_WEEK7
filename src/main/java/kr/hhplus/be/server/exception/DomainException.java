package kr.hhplus.be.server.exception;

public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}