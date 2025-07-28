package kr.hhplus.be.server.waiting.port.in;


import kr.hhplus.be.server.waiting.infra.web.dto.WaitingStatusRespone;

public interface WaitingUseCase {
    String enterWaiting();
    WaitingStatusRespone getStatus(String userID);
}
