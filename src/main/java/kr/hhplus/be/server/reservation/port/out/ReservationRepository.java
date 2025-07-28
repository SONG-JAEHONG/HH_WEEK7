package kr.hhplus.be.server.reservation.port.out;

import kr.hhplus.be.server.reservation.domain.Reservation;

import java.util.Optional;

public interface ReservationRepository {
        Optional<Reservation> findById(Long reservationId);

        void save(Reservation reservation);
}
