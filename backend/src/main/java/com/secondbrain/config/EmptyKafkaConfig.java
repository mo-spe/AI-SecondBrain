package com.secondbrain.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/** Kafka空配置. <p>Kafka 关闭时的占位配置类</p> */
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EmptyKafkaConfig {
}
