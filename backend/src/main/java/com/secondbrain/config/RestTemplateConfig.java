package com.secondbrain.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/** RestTemplate配置. <p>配置带日志拦截器的 RestTemplate</p> */
@Configuration
public class RestTemplateConfig {

    private final LoggingInterceptor loggingInterceptor;

    /**
     * 构造器注入日志拦截器.
     *
     * @param loggingInterceptor HTTP 请求日志拦截器
     */
    public RestTemplateConfig(LoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    /**
     * 创建带日志拦截器的 RestTemplate.
     *
     * @param builder RestTemplate 构造器
     * @return 配置好的 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(60))
                .setReadTimeout(Duration.ofSeconds(600))
                .additionalInterceptors(loggingInterceptor)
                .build();
    }
}
