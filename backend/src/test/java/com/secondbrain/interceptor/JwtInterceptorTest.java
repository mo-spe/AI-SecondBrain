package com.secondbrain.interceptor;

import com.secondbrain.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 拦截器认证边界测试。
 *
 * @author AI
 */
class JwtInterceptorTest {

    @Test
    void shouldRejectRequestWithoutBearerToken() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter payload = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(payload));

        boolean allowed = new JwtInterceptor(jwtUtil).preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(payload.toString()).contains("\"code\":401");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldPopulateIdentityForValidToken() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(12L);
        when(jwtUtil.getRoleFromToken("valid-token")).thenReturn("user");
        when(jwtUtil.getCurrentWsIdFromToken("valid-token")).thenReturn(7L);

        boolean allowed = new JwtInterceptor(jwtUtil).preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        verify(request).setAttribute("userId", 12L);
        verify(request).setAttribute("workspaceId", 7L);
    }
}
