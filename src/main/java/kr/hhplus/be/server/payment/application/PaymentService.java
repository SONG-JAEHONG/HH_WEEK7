package kr.hhplus.be.server.payment.application;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.infra.event.DecrRemainSeatAfterPaymentEvent;
import kr.hhplus.be.server.concert.port.out.SeatRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.port.in.PaymentUseCase;
import kr.hhplus.be.server.payment.port.out.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.port.out.ReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentUseCase {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public void pay(Long userId, Long reservationId, Long amount) {
        User user = userRepository.findUserByIdOrThrow(userId);
        user.usePoint(amount);
        Reservation reservation = reservationRepository.findReservationByIdOrThrow(reservationId);

        Payment payment = new Payment(null, amount, user, reservation, PaymentStatus.SUCCESS);

        Seat seat = reservation.getSeat();
        seat.reserve();
        seatRepository.save(seat);

        reservation.reserve();
        reservationRepository.save(reservation);

        paymentRepository.save(payment);

        Long ConcertDateId = seat.getConcertDate().getId();
        Long ConcertId = seat.getConcertDate().getConcert().getId();
        publisher.publishEvent(new DecrRemainSeatAfterPaymentEvent(reservation.getId(),ConcertId,ConcertDateId));
    }
}
