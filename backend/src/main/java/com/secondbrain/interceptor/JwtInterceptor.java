package com.secondbrain.interceptor;

import com.secondbrain.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

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
     * 请求预处理，校验 JWT Token 并将用户信息存入请求属性.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 登录态有效时放行；缺少或失效时返回 401 并阻断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return rejectUnauthorized(response, "请先登录");
        }

        try {
            String bearerToken = token.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(bearerToken);
            if (userId == null) {
                return rejectUnauthorized(response, "登录已失效，请重新登录");
            }
            request.setAttribute("userId", userId);

            String role = jwtUtil.getRoleFromToken(bearerToken);
            if (role != null) {
                request.setAttribute("role", role);
            }

            Long currentWsId = jwtUtil.getCurrentWsIdFromToken(bearerToken);
            if (currentWsId != null) {
                request.setAttribute("currentWsId", currentWsId);
                request.setAttribute("workspaceId", currentWsId);
            }

            log.debug("jwt_parsed userId={} role={} currentWsId={}", userId, role, currentWsId);
            return true;
        } catch (Exception exception) {
            // 受保护接口必须显式失败，避免下游把空 userId 误解为用户没有数据。
            log.warn("jwt_rejected reason={}", exception.getMessage());
            return rejectUnauthorized(response, "登录已失效，请重新登录");
        }
    }

    private boolean rejectUnauthorized(HttpServletResponse response, String message) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        } catch (IOException exception) {
            log.warn("write_unauthorized_response_failed", exception);
        }
        return false;
    }
}
