package kr.hhplus.be.server.payment.application;


import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.port.out.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.port.out.ReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private UserRepository userRepository;
    private ReservationRepository reservationRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        userRepository = mock(UserRepository.class);
        reservationRepository = mock(ReservationRepository.class);

        paymentService = new PaymentService(userRepository, paymentRepository,  reservationRepository);
    }

    @Test
    void 결제에_성공시_포인트_차감_결제내역_저장() {
        // given
        Long userId = 1L;
        Long reservationId = 10L;
        Long amount = 500L;

        User user = new User(userId, 1000L);
        Reservation reservation = new Reservation();  // 필요한 경우 필드 초기화

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when
        paymentService.pay(userId, reservationId, amount);

        // then
        assertThat(user.getPoint()).isEqualTo(500L);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void 포인트_부족시_예외발생_결제내역_저장되지_않는다() {
        // given
        Long userId = 1L;
        Long reservationId = 10L;
        Long amount = 1500L;

        User user = new User(userId, 1000L);
        Reservation reservation = new Reservation();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.pay(userId, reservationId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트가 부족");

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
