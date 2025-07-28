package kr.hhplus.be.server.user.application;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {

        userService = new UserService(userRepository);
    }

    @Test
    void 포인트_충전() {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        User user = new User(userId, 500L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.chargePoint(userId, amount);

        // then
        assertThat(user.getPoint()).isEqualTo(1500L);
        verify(userRepository).findById(userId);
    }

    @Test
    void usePoint_success() {
        // given
        Long userId = 1L;
        Long amount = 300L;
        User user = new User(userId, 1000L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.usePoint(userId, amount);

        // then
        assertEquals(700L, user.getPoint());
        verify(userRepository).findById(userId);
    }

    @Test
    void usePoint_insufficient() {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        User user = new User(userId, 500L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.usePoint(userId, amount));

        assertEquals("포인트가 부족합니다.", exception.getMessage());
        verify(userRepository).findById(userId);
    }
}