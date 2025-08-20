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

        String remainKey = RedisKeys.RemainSeat(concertDateId);
        String totalKey = RedisKeys.TotalSeat(concertDateId);

        Boolean created = redis.opsForValue().setIfAbsent(remainKey, String.valueOf(totalSeats));
        redis.opsForValue().setIfAbsent(totalKey, String.valueOf(totalSeats));

        if (Boolean.TRUE.equals(created) && ttl != null) {

            redis.expire(remainKey, ttl);

        }
    }

    public enum Result { NONE, SOLD_OUT, UNDERFLOW }


    public Result decrRemainSeat(long concertDateId, String member) {

        String remainKey = RedisKeys.RemainSeat(concertDateId);
        String totalKey = RedisKeys.TotalSeat(concertDateId);

        Long remain = redis.opsForValue().decrement(remainKey);
        if (remain == null) return Result.UNDERFLOW;
        if (remain < 0)    return Result.UNDERFLOW;


        String totalStr = redis.opsForValue().get(totalKey);
        long total = (totalStr == null ? 0 : Long.parseLong(totalStr));

        if(total>0){
            double ratio = (double)(total - remain) /  (double) total;
            redis.opsForZSet().add(RedisRankKeys.ratioAll(),member,ratio);
        }

        if (remain == 0)   return Result.SOLD_OUT;

        return Result.NONE;
    }
}