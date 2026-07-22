package com.secondbrain.config;

import com.secondbrain.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，用于注册拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    /**
     * 添加拦截器配置。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/chat/**", "/knowledge/**", "/review/**", "/deerflow/**", "/rag/**", "/report/**", "/async-task/**")
                .excludePathPatterns("/auth/**", "/health/**");
    }
}
