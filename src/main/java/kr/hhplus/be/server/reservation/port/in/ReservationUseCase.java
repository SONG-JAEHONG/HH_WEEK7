package kr.hhplus.be.server.reservation.port.in;

import kr.hhplus.be.server.reservation.infra.web.dto.ReservationRequest;
import kr.hhplus.be.server.reservation.infra.web.dto.ReservationResponse;

public interface ReservationUseCase {

    ReservationResponse reserve(ReservationRequest reservationRequest, Long userId);

}
