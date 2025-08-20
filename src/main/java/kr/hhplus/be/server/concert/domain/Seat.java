package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.base.BaseTimeEntity;
import lombok.Builder;
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

    @Version
    private Long version;

    // concertRepositoryAdapter 테스트용
    public Seat(ConcertDate concertDate, int seatNumber, SeatStatus seatStatus) {

        this.concertDate = concertDate;
        this.seatNumber = seatNumber;
        this.status = seatStatus;
    }

    @Builder
    private Seat(ConcertDate concertDate, Integer number, SeatStatus status) {
        this.concertDate = concertDate;
        this.seatNumber = number;
        this.status = status;
    }


    // concertService 테스트용
    public Seat(long l, ConcertDate concertDate, int i, SeatStatus seatStatus, LocalDateTime localDateTime) {

        this.id = l;
        this.concertDate = concertDate;
        this.seatNumber = i;
        this.status = seatStatus;
        this.expireTime = localDateTime;
    }

    // concertIntegration 테스트용

    public Seat(ConcertDate concertDate, int i, SeatStatus seatStatus, Object o) {

        this.concertDate = concertDate;
        this.seatNumber = i;
        this.status = seatStatus;

        if (o instanceof LocalDateTime) {
            this.expireTime = (LocalDateTime) o;
        } else {
            this.expireTime = null;
        }
    }




    public void hold(LocalDateTime expiresAt) {
        if (!isAvailable()) {
            throw new IllegalStateException("이미 예약(선점)된 좌석입니다.");
        }
        this.status = SeatStatus.HOLDING;
        this.expireTime = expiresAt;
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
