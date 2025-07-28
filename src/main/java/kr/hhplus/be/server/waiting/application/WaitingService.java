package kr.hhplus.be.server.waiting.application;

import kr.hhplus.be.server.waiting.infra.web.dto.WaitingStatusRespone;
import kr.hhplus.be.server.waiting.port.in.WaitingUseCase;
import kr.hhplus.be.server.waiting.port.out.WaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitingService implements WaitingUseCase {

    private final WaitingRepository waitingRepository;

    @Override
    public String enterWaiting() {
        String userId = UUID.randomUUID().toString();
        waitingRepository.enterWaiting(userId);
        waitingRepository.fillWorking(); //빈자리 채우기
        return userId;
    }

    @Override
    public WaitingStatusRespone getStatus(String userID) {
        int position = waitingRepository.getPosition(userID);
        return new WaitingStatusRespone(position, false);
    }
}
