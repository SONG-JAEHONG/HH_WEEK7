package kr.hhplus.be.server.concert.infra.event.listener;

import kr.hhplus.be.server.concert.infra.event.DecrRemainSeatAfterPaymentEvent;
import kr.hhplus.be.server.concert.infra.redis.RemainSeatCount;
import kr.hhplus.be.server.concert.port.in.SellOutUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DecrRemainSeatAfterPaymentEventListener {

    private final RemainSeatCount remainSeatCount;
    private final SellOutUseCase sellOutUseCase;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(DecrRemainSeatAfterPaymentEvent e) {
        String member = e.concertId() + ":" + e.concertDateId();
        RemainSeatCount.Result r = remainSeatCount.decrRemainSeat(e.concertDateId(), member);
        if (r == RemainSeatCount.Result.SOLD_OUT) {
            sellOutUseCase.recordRank(e.concertDateId());
        }
    }

}
