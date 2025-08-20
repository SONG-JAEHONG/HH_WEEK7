package kr.hhplus.be.server.concert.infra.redis;

import java.time.LocalDateTime;

public interface SelloutRankWriter {

    void pushFastAll(Long concertId, Long concertDateId, long selloutSeconds, LocalDateTime selloutAt);

}
