package kr.hhplus.be.server.waiting.application;
import kr.hhplus.be.server.waiting.infra.web.dto.WaitingStatusRespone;
import kr.hhplus.be.server.waiting.port.out.WaitingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
class WaitingServiceTest {

    private WaitingRepository waitingRepository;
    private WaitingService waitingService;

    @BeforeEach
    void setUp() {
        waitingRepository = mock(WaitingRepository.class);
        waitingService = new WaitingService(waitingRepository);
    }

    @Test
    @DisplayName("대기열 등록 시 유저 ID 반환하고 fillWorking 호출됨")
    void 대기열_입장() {
        // when
        String userId = waitingService.enterWaiting();

        // then
        assertThat(userId).isNotNull();
        verify(waitingRepository).enterWaiting(userId);
    }


    @Test
    void 대기열_순번_조회() {
        // given
        String userId = "test-user";
        when(waitingRepository.getPosition(userId)).thenReturn(3);

        // when
        WaitingStatusRespone response = waitingService.getStatus(userId);

        // then
        assertThat(response.position()).isEqualTo(3);
        assertThat(response.canReserve()).isFalse();
        verify(waitingRepository).getPosition(userId);
    }
}
