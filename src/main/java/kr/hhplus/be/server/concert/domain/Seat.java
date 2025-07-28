package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.base.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Seat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int seatNumber;


    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_date_id")
    private ConcertDate concertDate;

    private LocalDateTime expireTime;


     public Seat(Long id, ConcertDate concertDate, int seatNumber, SeatStatus seatStatus, LocalDateTime expireTime) {
        this.id = id;
        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
        this.status = seatStatus;
        this.expireTime = expireTime;
     }

    public void hold() {
        if (!isAvailable()) {
            throw new IllegalStateException("이미 예약된 좌석입니다.");
        }
        this.status = SeatStatus.HOLDING;
        this.expireTime = LocalDateTime.now().plusMinutes(5);
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public void reserve() {
        if (this.status != SeatStatus.HOLDING) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }
        this.status = SeatStatus.RESERVED;
    }



    public void release() {

         if(this.status != SeatStatus.HOLDING){
             throw new IllegalStateException("임시 배정 좌석이 아닙니다.");
         }
         this.status = SeatStatus.AVAILABLE;
         this.expireTime = null;
    }

}
