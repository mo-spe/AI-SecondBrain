package com.secondbrain.interceptor;

import com.secondbrain.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        
        log.info("JWT拦截器检查，request URI: {}, Authorization header: {}", 
            request.getRequestURI(), 
            token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                log.info("JWT 解析结果，userId: {}", userId);
                if (userId != null) {
                    request.setAttribute("userId", userId);
                    log.info("JWT 验证成功，userId: {}", userId);
                    return true;
                } else {
                    log.warn("JWT 解析成功但 userId 为 null");
                }
            } catch (Exception e) {
                log.error("JWT 验证失败：{}", e.getMessage(), e);
            }
        } else {
            log.warn("没有找到有效的 Authorization header");
        }
        
        return true;
    }
}
