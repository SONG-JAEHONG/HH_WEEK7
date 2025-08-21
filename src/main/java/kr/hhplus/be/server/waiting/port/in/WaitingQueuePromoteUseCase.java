package kr.hhplus.be.server.waiting.port.in;

import kr.hhplus.be.server.waiting.application.WaitingQueuePromoteService;

public interface WaitingQueuePromoteUseCase {

    PromoteResult promoteOnce();

    record PromoteResult(long moved, java.util.List<String> tokens) {}


}
