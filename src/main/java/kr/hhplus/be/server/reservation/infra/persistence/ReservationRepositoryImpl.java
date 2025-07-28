package kr.hhplus.be.server.reservation.infra.persistence;


import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.reservation.domain.Reservation;
import kr.hhplus.be.server.reservation.port.out.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepository {

    private EntityManager em;

    @Override
    public Optional<Reservation> findById(Long reservationId) {
        try {
            Reservation result = em.createQuery("SELECT r FROM Reservation r WHERE r.id = :reservationId", Reservation.class)
                    .setParameter("reservationId", reservationId)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public void save(Reservation reservation){
        em.persist(reservation);
    }


}
