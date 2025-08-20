package kr.hhplus.be.server.common.config;

import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = false)
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:127.0.0.1}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${app.redisson.watchdog-timeout-ms:30000}") long watchdogTimeoutMs
    ) {
        Config config = new Config();
        SingleServerConfig s = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(0)
                .setTimeout(10000)
                .setConnectTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        if (password != null && !password.isBlank()) {
            s.setPassword(password);
        }
        config.setLockWatchdogTimeout(watchdogTimeoutMs);
        return Redisson.create(config);
    }
}

