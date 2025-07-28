package kr.hhplus.be.server.reservation.infra.web.controller;

import kr.hhplus.be.server.reservation.infra.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.infra.web.dto.ReservationResponse;
import kr.hhplus.be.server.reservation.port.in.ReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(
            @RequestBody ReservationRequest request
    ) {
        ReservationResponse response = reservationUseCase.reserve(request, request.userId());
        return ResponseEntity.ok(response);
    }
}
