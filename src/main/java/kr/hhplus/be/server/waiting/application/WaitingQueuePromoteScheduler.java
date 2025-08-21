package kr.hhplus.be.server.waiting.application;


import kr.hhplus.be.server.waiting.port.in.WaitingQueuePromoteUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingQueuePromoteScheduler {

    private final WaitingQueuePromoteUseCase waitingQueuePromoteUseCase;


    @Scheduled(fixedDelayString = "${queue.promote.fixedDelay:100}")
    public void tick() {
        var result = waitingQueuePromoteUseCase.promoteOnce();
        if (result.moved() > 0) {
            log.debug("Promoted {} users: {}", result.moved(), result.tokens());
        }
    }
}