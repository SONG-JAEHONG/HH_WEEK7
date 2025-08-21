package kr.hhplus.be.server.waiting.port.in;

public interface WaitingQueueCompleteUseCase {
    record CompleteResult(String userId, boolean removedFromWorking) {}
    CompleteResult complete(String token);
}
