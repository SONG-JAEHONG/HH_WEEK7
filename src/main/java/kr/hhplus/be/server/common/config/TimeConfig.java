package kr.hhplus.be.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() { return Clock.systemUTC(); } // 또는 Asia/Seoul
}