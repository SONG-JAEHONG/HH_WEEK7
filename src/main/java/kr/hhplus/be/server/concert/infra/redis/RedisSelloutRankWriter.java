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
    public void pushDaily(Long concertId, Long concertDateId, long selloutSeconds, LocalDateTime selloutAt) {
        String member = RedisRankKeys.member(concertId, concertDateId);
        double score = -1.0 * selloutSeconds;
        String dailyKey = RedisRankKeys.daily(selloutAt);

        redis.opsForZSet().add(dailyKey, member, score);
    }
}
