package kr.hhplus.be.server.concert.infra.persistence;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatJpaRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    List<Seat> findByConcertDateIdAndStatus(Long concertDateId, SeatStatus status);
    List<Seat> findByStatusAndExpireTimeBefore(SeatStatus status, LocalDateTime time);
}
