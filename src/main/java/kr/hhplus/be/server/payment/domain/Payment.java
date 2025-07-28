package kr.hhplus.be.server.payment.domain;


import jakarta.persistence.*;
import kr.hhplus.be.server.common.base.BaseTimeEntity;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @OneToOne
    @JoinColumn(name = "reservationId")
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;



}
