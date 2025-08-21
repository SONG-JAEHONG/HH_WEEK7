package kr.hhplus.be.server.concert.application;


import kr.hhplus.be.server.concert.domain.ConcertDate;
import kr.hhplus.be.server.concert.infra.persistence.ConcertDateJpaRepository;
import kr.hhplus.be.server.concert.infra.redis.SelloutRankWriter;
import kr.hhplus.be.server.concert.port.in.SellOutUseCase;
import kr.hhplus.be.server.concert.port.out.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellOutService implements SellOutUseCase {

    private final ConcertRepository concertRepository;
    private final SelloutRankWriter rankWriter;
    private final Clock clock;

    @Transactional
    public void recordRank(Long concertDateId) {

        LocalDateTime now = LocalDateTime.now(clock);
        ConcertDate cd = concertRepository.findConcertDateByIdOrThrow(concertDateId);

        long seconds = Duration.between(cd.getOpenAt(), now).getSeconds();

        int updated = concertRepository.updateSellOut(concertDateId, now, seconds);

        if(updated == 1){
            Long concertId = cd.getConcert().getId();
            rankWriter.pushFastAll(concertId, concertDateId, seconds, now);
        }

    }
}