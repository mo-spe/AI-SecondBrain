package com.secondbrain.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

    /**
     * 创建用于SSE流式调用的 RestTemplate.
     *
     * <p>不添加 LoggingInterceptor，因为 SSE 响应是持续流，不能缓冲整个响应体。
     * 同时设置 bufferRequestBody=false 避免请求体被缓冲</p>
     *
     * @return 无拦截器的 RestTemplate
     */
    @Bean("streamingRestTemplate")
    public RestTemplate streamingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(false);
        factory.setConnectTimeout(60_000);
        factory.setReadTimeout(600_000);
        return new RestTemplate(factory);
    }

    /**
     * 创建用于第三方 OAuth 换票的无日志客户端。
     *
     * <p>微信换票 URL 携带 app-secret，不能经过通用请求日志拦截器，避免凭证进入应用日志。</p>
     *
     * @return 无敏感信息日志的短超时客户端
     */
    @Bean("oauthRestTemplate")
    public RestTemplate oauthRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setBufferRequestBody(false);
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }
}
