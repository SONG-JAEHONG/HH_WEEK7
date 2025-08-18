package kr.hhplus.be.server.reservation.port.out;

import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.exception.ReservationErrorCode;
import kr.hhplus.be.server.reservation.exception.ReservationException;

import java.util.Optional;

public interface ReservationRepository {

        Optional<Reservation> findReservationById(Long reservationId);

        void save(Reservation reservation);

        default Reservation findReservationByIdOrThrow(Long reservationId){
                return findReservationById(reservationId).orElseThrow(() -> new ReservationException(
                        ReservationErrorCode.RESERVATION_NOT_FOUND, "존재하지 않는 예약입니다. reservationId=" + reservationId));
        }

}
