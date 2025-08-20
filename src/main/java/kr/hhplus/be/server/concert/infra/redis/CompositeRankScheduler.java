package kr.hhplus.be.server.concert.infra.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CompositeRankScheduler {

    private final StringRedisTemplate redis;


    private static final double W_FAST  = 0.6;
    private static final double W_RATIO = 0.4;


    @Scheduled(fixedDelay = 1000)
    public void rebuildCompositeCache() {

        final String fastKey = RedisRankKeys.fastAll();
        final String ratioKey = RedisRankKeys.ratioAll();
        final String destKey  = RedisRankKeys.compositeCache();


        redis.execute((RedisCallback<Object>) (RedisConnection connection) -> {
            byte[] dest  = destKey.getBytes(StandardCharsets.UTF_8);
            byte[] fast  = fastKey.getBytes(StandardCharsets.UTF_8);
            byte[] ratio = ratioKey.getBytes(StandardCharsets.UTF_8);


            connection.execute("ZUNIONSTORE",
                    dest,
                    "2".getBytes(StandardCharsets.UTF_8),
                    fast, ratio,
                    "WEIGHTS".getBytes(StandardCharsets.UTF_8),
                    toAscii(W_FAST), toAscii(W_RATIO),
                    "AGGREGATE".getBytes(StandardCharsets.UTF_8),
                    "SUM".getBytes(StandardCharsets.UTF_8)
            );

            connection.keyCommands().expire(dest, 2);
            return null;
        });
    }

    private static byte[] toAscii(double v) {

        String s = String.format(Locale.US, "%.6f", v);
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
