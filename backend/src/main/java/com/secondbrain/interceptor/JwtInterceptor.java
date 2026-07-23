package com.secondbrain.interceptor;

import com.secondbrain.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT拦截器. <p>用于验证请求中的 JWT Token 并提取用户信息</p> */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    private final JwtUtil jwtUtil;

    /**
     * 构造器注入 JwtUtil.
     *
     * @param jwtUtil JWT 工具类
     */
    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 请求预处理，解析 JWT Token 并将用户信息存入请求属性.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 始终返回 true 以放行请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);

            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    request.setAttribute("userId", userId);

                    String role = jwtUtil.getRoleFromToken(token);
                    if (role != null) {
                        request.setAttribute("role", role);
                    }

                    Long currentWsId = jwtUtil.getCurrentWsIdFromToken(token);
                    if (currentWsId != null) {
                        request.setAttribute("currentWsId", currentWsId);
                        request.setAttribute("workspaceId", currentWsId);
                    }

                    log.debug("jwt_parsed userId={} role={} currentWsId={}", userId, role, currentWsId);
                }
            } catch (Exception e) {
                log.error("JWT 解析失败：{}", e.getMessage());
            }
        }

        return true;
    }
}
