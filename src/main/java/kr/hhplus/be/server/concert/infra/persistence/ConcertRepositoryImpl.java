package kr.hhplus.be.server.concert.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConcertRepositoryImpl implements ConcertRepository {


    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Concert> findAll() {
        return em.createQuery("SELECT c FROM Concert c", Concert.class)
                .getResultList();
    }

    @Override
    public List<ConcertDate> findByConcertId(Long concertId) {
        return em.createQuery(
                        "SELECT cd FROM ConcertDate cd WHERE cd.concert.id = :concertId", ConcertDate.class)
                .setParameter("concertId", concertId)
                .getResultList();
    }

    @Override
    public List<Seat> findAvailableSeatsByConcertDateId(Long concertDateId, SeatStatus status) {
        return em.createQuery(
                        "SELECT s FROM Seat s WHERE s.concertDate.id = :concertDateId AND s.status = :status", Seat.class)
                .setParameter("concertDateId", concertDateId)
                .setParameter("status", status)
                .getResultList();
    }
    @Override
    public Optional<ConcertDate> findConcertDateById(Long concertDateId) {
        try {
            ConcertDate result = em.createQuery(
                            "SELECT cd FROM ConcertDate cd WHERE cd.id = :concertDateId", ConcertDate.class)
                    .setParameter("concertDateId", concertDateId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

}
