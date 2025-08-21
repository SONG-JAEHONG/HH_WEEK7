package kr.hhplus.be.server.waiting.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PromoteProps.class)
public class QueueConfig { }
