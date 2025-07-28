package kr.hhplus.be.server.user.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.port.out.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager em;


    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(em.createQuery("SELECT u FROM User u WHERE u.userId = :userId", User.class)
                .setParameter("userId", userId)
                .getSingleResult());
    }
}
