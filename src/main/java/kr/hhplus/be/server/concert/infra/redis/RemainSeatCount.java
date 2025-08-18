package kr.hhplus.be.server.concert.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RemainSeatCount {

    private final StringRedisTemplate redis;

    public void initRemainSeat(long concertDateId, long totalSeats, Duration ttl) {

        String key = RedisKeys.RemainSeat(concertDateId);

        Boolean created = redis.opsForValue().setIfAbsent(key, String.valueOf(totalSeats));

        if (Boolean.TRUE.equals(created) && ttl != null) {

            redis.expire(key, ttl);

        }
    }

    public enum Result { NONE, SOLD_OUT, UNDERFLOW }


    public Result decrRemainSeat(long concertDateId) {
        Long remain = redis.opsForValue().decrement(RedisKeys.RemainSeat(concertDateId));
        if (remain == null) return Result.UNDERFLOW;
        if (remain == 0)   return Result.SOLD_OUT;
        if (remain < 0)    return Result.UNDERFLOW;
        return Result.NONE;
    }
}