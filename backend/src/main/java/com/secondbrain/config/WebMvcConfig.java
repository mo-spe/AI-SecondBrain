package com.secondbrain.config;

import com.secondbrain.interceptor.JwtInterceptor;
import com.secondbrain.interceptor.WorkspaceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC配置类. <p>用于注册拦截器</p> */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final WorkspaceInterceptor workspaceInterceptor;

    /**
     * 构造器注入 JWT 拦截器和工作区拦截器.
     *
     * @param jwtInterceptor       JWT 拦截器
     * @param workspaceInterceptor 工作区拦截器
     */
    public WebMvcConfig(JwtInterceptor jwtInterceptor, WorkspaceInterceptor workspaceInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.workspaceInterceptor = workspaceInterceptor;
    }

    /**
     * 添加拦截器配置。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/chat/**", "/knowledge/**", "/review/**", "/deerflow/**", "/rag/**", "/report/**", "/async-task/**", "/workspace/**", "/admin/**", "/share/**", "/square/**", "/notification/**", "/statistics/**", "/user/**", "/gamification/**", "/tags/**", "/ai/**")
                .excludePathPatterns("/auth/**", "/health/**");

        registry.addInterceptor(workspaceInterceptor)
                .addPathPatterns("/workspace/**");
    }
}
