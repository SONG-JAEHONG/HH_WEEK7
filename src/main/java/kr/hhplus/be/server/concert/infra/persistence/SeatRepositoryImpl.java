package kr.hhplus.be.server.concert.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.port.out.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class SeatRepositoryImpl implements SeatRepository {

    private EntityManager em;

    @Override
    public Optional<Seat> findById(Long id) {
        try{
        Seat result = em.createQuery("SELECT s FROM Seat s WHERE s.id = id", Seat.class)
                .setParameter("id",id)
                .getSingleResult();
            return Optional.of(result);
        }catch (NoResultException e){
            return Optional.empty();
        }
    }

    @Override
    public void  save(Seat seat) {
        em.persist(seat);
    }

    @Override
    public List<Seat> findByStatusAndBeforeExpire(SeatStatus status, LocalDateTime time) {
        return em.createQuery(
                "SELECT s FROM SEAT s WHERE s.status = :status AND s.expireTime <= :time", Seat.class)
                .setParameter("status", status)
                .setParameter("time", time)
                .getResultList();

    }
}
