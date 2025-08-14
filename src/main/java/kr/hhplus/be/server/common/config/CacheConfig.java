package kr.hhplus.be.server.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    private RedisCacheConfiguration baseConfig(Duration ttl) {

        var serializer = new GenericJackson2JsonRedisSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .entryTtl(ttl);
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory cf) {
        Map<String, RedisCacheConfiguration> conf = new HashMap<>();

        conf.put("concert:list",  baseConfig(Duration.ofMinutes(10)));
        conf.put("concert:dates", baseConfig(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(baseConfig(Duration.ofSeconds(30)))
                .withInitialCacheConfigurations(conf)
                .build();
    }
}