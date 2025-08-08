package kr.hhplus.be.server.user.exception;

import kr.hhplus.be.server.exception.BusinessException;


public class UserException extends BusinessException {

    private final UserErrorCode userErrorCode;


    public UserException(UserErrorCode errorCode, String message) {
        super(message);
        this.userErrorCode = errorCode;
    }

    public UserErrorCode getErrorCode() {
        return userErrorCode;
    }


}
