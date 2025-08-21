package kr.hhplus.be.server.concert.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RedisSelloutRankWriter implements SelloutRankWriter {

    private final StringRedisTemplate redis;

    @Override
    public void pushFastAll(Long concertId, Long concertDateId, long selloutSeconds, LocalDateTime selloutAt) {
        String member = RedisRankKeys.member(concertId, concertDateId);
        double baseSec = 24 * 60 * 60;
        double fastNorm = 1.0 - Math.min(1.0, Math.max(0.0, selloutSeconds / baseSec));
        redis.opsForZSet().add(RedisRankKeys.fastAll(), member, fastNorm);
    }
}
