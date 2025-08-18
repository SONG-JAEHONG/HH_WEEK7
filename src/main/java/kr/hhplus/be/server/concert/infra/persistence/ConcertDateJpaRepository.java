package kr.hhplus.be.server.concert.infra.persistence;

import kr.hhplus.be.server.concert.domain.ConcertDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConcertDateJpaRepository extends JpaRepository<ConcertDate, Long> {

    List<ConcertDate> findByConcertId(Long concertId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ConcertDate cd
           SET cd.selloutAt = :now,
               cd.selloutSeconds = :seconds
         WHERE cd.id = :dateId
           AND cd.selloutAt IS NULL
    """)
    int updateSellOut(@Param("dateId") Long dateId,
                           @Param("now") LocalDateTime now,
                           @Param("seconds") long seconds);

}
