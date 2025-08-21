package kr.hhplus.be.server.concert.infra.event.listener;

import kr.hhplus.be.server.concert.infra.event.DecrRemainSeatAfterPaymentEvent;
import kr.hhplus.be.server.concert.infra.redis.RemainSeatCount;
import kr.hhplus.be.server.concert.port.in.SellOutUseCase;
import kr.hhplus.be.server.reservation.port.out.ReservationRepository;
import kr.hhplus.be.server.waiting.infra.redis.WaitingQueueKeys;
import kr.hhplus.be.server.waiting.port.in.WaitingQueueCompleteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DecrRemainSeatAfterPaymentEventListener {

    private final RemainSeatCount remainSeatCount;
    private final SellOutUseCase sellOutUseCase;
    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redis;
    private final WaitingQueueCompleteUseCase queueComplete;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(DecrRemainSeatAfterPaymentEvent e) {
        String member = e.concertId() + ":" + e.concertDateId();
        RemainSeatCount.Result r = remainSeatCount.decrRemainSeat(e.concertDateId(), member);
        if (r == RemainSeatCount.Result.SOLD_OUT) {
            sellOutUseCase.recordRank(e.concertDateId());
        }
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaid(DecrRemainSeatAfterPaymentEvent e) {
        try {

            var reservation = reservationRepository.findReservationByIdOrThrow(e.reservationId());
            Long userId = reservation.getUser().getId();


            Object tokenObj = redis.opsForHash().get(WaitingQueueKeys.activeTokenH(), String.valueOf(userId));
            if (tokenObj == null) {
                log.info("[queue] no active token for userId={}, skip cleanup", userId);
                return;
            }
            String token = String.valueOf(tokenObj);


            var res = queueComplete.complete(token);
            log.info("[queue] payment done -> cleaned token={}, userId={}, removedFromWorking={}",
                    token, res.userId(), res.removedFromWorking());
        } catch (Exception ex) {

            log.error("[queue] cleanup after payment failed", ex);
        }
    }

}
