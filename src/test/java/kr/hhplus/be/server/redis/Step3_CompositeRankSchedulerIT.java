package kr.hhplus.be.server.redis;

import kr.hhplus.be.server.concert.infra.redis.CompositeRankScheduler;
import kr.hhplus.be.server.concert.infra.redis.RedisRankKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@SpringBootTest(properties = {
        // 스케줄러 자동 실행 방지 (직접 호출로 검증)
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class CompositeRankSchedulerIT {

    @Autowired CompositeRankScheduler scheduler;
    @Autowired StringRedisTemplate redis;

    @AfterEach
    void clean() {
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void ZUNIONSTORE_컴포지트캐시_빌드_점수계산_TTL_덮어쓰기() {

        String fastKey = RedisRankKeys.fastAll();
        String ratioKey = RedisRankKeys.ratioAll();
        String destKey  = RedisRankKeys.compositeCache();


        redis.delete(fastKey);
        redis.delete(ratioKey);
        redis.delete(destKey);


        redis.opsForZSet().add(destKey, "noise", 999d);


        redis.opsForZSet().add(fastKey, "cd:1", 0.50);
        redis.opsForZSet().add(ratioKey, "cd:1", 0.20);


        redis.opsForZSet().add(fastKey, "cd:2", 0.30);


        redis.opsForZSet().add(ratioKey, "cd:3", 0.50);


        scheduler.rebuildCompositeCache();


        Double s1 = redis.opsForZSet().score(destKey, "cd:1");
        Double s2 = redis.opsForZSet().score(destKey, "cd:2");
        Double s3 = redis.opsForZSet().score(destKey, "cd:3");

        assertThat(s1).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(s3).isNotNull();

        assertThat(s1).isCloseTo(0.38, offset(1e-9));
        assertThat(s2).isCloseTo(0.18, offset(1e-9));
        assertThat(s3).isCloseTo(0.20, offset(1e-9));


        assertThat(redis.opsForZSet().score(destKey, "noise")).isNull();


        Long ttlSec = redis.getExpire(destKey, TimeUnit.SECONDS);
        assertThat(ttlSec).isNotNull();
        assertThat(ttlSec).isGreaterThan(0L);
    }
}
