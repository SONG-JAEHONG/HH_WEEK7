package kr.hhplus.be.server.user.port.out;

import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.exception.UserErrorCode;
import kr.hhplus.be.server.user.exception.UserException;

import java.util.Optional;


public interface UserRepository  {
    Optional<User> findUserById(Long userId);

    default User findUserByIdOrThrow(Long userId){
        return findUserById(userId).orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다. id=" + userId));
    }
}
