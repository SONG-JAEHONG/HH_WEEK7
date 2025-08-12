package kr.hhplus.be.server.common.config;

import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}") String host;
    @Value("${spring.redis.port}") int port;
    @Value("${app.redisson.watchdog-timeout-ms:30000}") long watchdogTimeoutMs;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        config.setLockWatchdogTimeout(watchdogTimeoutMs);
        return Redisson.create(config);
    }
}

