package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.concert.infra.redis.RemainSeatCount;
import kr.hhplus.be.server.concert.infra.redis.RedisKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RemainSeatCountIT {

    @Autowired RemainSeatCount remainSeatCount;
    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void initRemainSeat_키생성_및_TTL설정() {
        long concertDateId = 9999L;
        long totalSeats = 150;
        Duration ttl = Duration.ofMinutes(5);

        remainSeatCount.initRemainSeat(concertDateId, totalSeats, ttl);

        String totalKey  = RedisKeys.TotalSeat(concertDateId);
        String remainKey = RedisKeys.RemainSeat(concertDateId);

        assertThat(redis.opsForValue().get(totalKey)).isEqualTo(String.valueOf(totalSeats));
        assertThat(redis.opsForValue().get(remainKey)).isEqualTo(String.valueOf(totalSeats));

        Long ttlSeconds = redis.getExpire(remainKey, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0L);
    }
}
