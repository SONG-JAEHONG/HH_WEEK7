package kr.hhplus.be.server.concert.infra.event.listener;

import kr.hhplus.be.server.concert.infra.event.ConcertDateCreateEvent;
import kr.hhplus.be.server.concert.infra.redis.RemainSeatCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ConcertDateCreateEventListener {

    private final RemainSeatCount remainSeatCount;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(ConcertDateCreateEvent concertDateCreateEvent) {
        remainSeatCount.initRemainSeat(concertDateCreateEvent.concertDateId(), concertDateCreateEvent.totalSeats(), Duration.ofDays(60));
    }

}
