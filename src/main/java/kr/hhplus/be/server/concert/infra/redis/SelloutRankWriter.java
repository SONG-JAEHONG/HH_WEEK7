package kr.hhplus.be.server.concert.infra.redis;

import java.time.LocalDateTime;

public interface SelloutRankWriter {

    void pushDaily(Long concertId, Long concertDateId, long selloutSeconds, LocalDateTime selloutAt);

}
