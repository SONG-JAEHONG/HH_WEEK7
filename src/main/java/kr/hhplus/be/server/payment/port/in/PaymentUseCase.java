package kr.hhplus.be.server.payment.port.in;

import kr.hhplus.be.server.user.domain.User;

public interface PaymentUseCase {
    void pay(Long userId, Long reservationId ,Long amount);

}
