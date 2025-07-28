package kr.hhplus.be.server.payment.infra.web.controller;

import kr.hhplus.be.server.payment.infra.web.dto.PaymentRequest;
import kr.hhplus.be.server.payment.port.in.PaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @PostMapping
    public ResponseEntity<Void> pay(@RequestBody PaymentRequest request) {
        paymentUseCase.pay(request.userId(), request.reservationId(), request.amount());
        return ResponseEntity.ok().build();
    }

}
