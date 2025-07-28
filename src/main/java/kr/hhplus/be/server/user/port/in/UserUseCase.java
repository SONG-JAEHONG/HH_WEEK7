package kr.hhplus.be.server.user.port.in;

public interface UserUseCase {

    void chargePoint(Long userId, Long amount);
    void usePoint(Long userId, Long amount);

}
