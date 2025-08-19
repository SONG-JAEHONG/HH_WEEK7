package kr.hhplus.be.server.concert.infra.persistence;

import kr.hhplus.be.server.concert.infra.redis.RedisRankKeys;
import kr.hhplus.be.server.concert.port.out.SelloutDailyRankReader;

import kr.hhplus.be.server.concert.port.out.SelloutDailyRankReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RedisSelloutDailyRankReader implements SelloutDailyRankReader {

    private final StringRedisTemplate redis;

    @Override
    public List<Map.Entry<String, Double>> top20(LocalDate date) {
        String key = RedisRankKeys.daily(date);


        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, 0, 19);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<String, Double>> result = new ArrayList<>(tuples.size());
        for (var t : tuples) {
            if (t.getValue() != null && t.getScore() != null) {
                result.add(Map.entry(t.getValue(), t.getScore()));
            }
        }
        return result;
    }
}