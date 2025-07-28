package kr.hhplus.be.server.waiting.port.out;

public interface WaitingRepository {

    void enterWaiting(String userId);

    void fillWorking();

    int getPosition(String userId);




}
