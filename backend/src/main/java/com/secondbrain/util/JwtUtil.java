package com.secondbrain.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类.
 * <p>提供JWT Token的生成、解析和验证功能</p>
 */
@Component
public class JwtUtil {

    /**
     * JWT签名密钥
     */
    @Value("${jwt.secret:ai-second-brain-secret-key-2024}")
    private String secret;

    /**
     * Token过期时间（毫秒）
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 获取签名密钥.
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token.
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null, null);
    }

    /**
     * 生成JWT Token（含角色和工作区信息）.
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param role 平台角色（super_admin/user）
     * @param currentWsId 当前选中的工作区ID
     * @return JWT Token
     */
    public String generateToken(Long userId, String username, String role, Long currentWsId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (role != null) {
            claims.put("role", role);
        }
        if (currentWsId != null) {
            claims.put("currentWsId", currentWsId);
        }
        return createToken(claims, username);
    }

    /**
     * 创建JWT Token.
     *
     * @param claims 自定义声明
     * @param subject 主题（用户名）
     * @return JWT Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从Token中获取所有声明.
     *
     * @param token JWT Token
     * @return 声明信息
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从Token中获取用户名.
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从Token中获取用户ID.
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取平台角色.
     *
     * @param token JWT Token
     * @return 平台角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 从Token中获取当前工作区ID.
     *
     * @param token JWT Token
     * @return 当前工作区ID
     */
    public Long getCurrentWsIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object wsId = claims.get("currentWsId");
        if (wsId == null) {
            return null;
        }
        if (wsId instanceof Integer) {
            return ((Integer) wsId).longValue();
        }
        return (Long) wsId;
    }

    /**
     * 从Token中获取过期时间.
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }

    /**
     * 判断Token是否过期.
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 验证Token是否有效.
     *
     * @param token JWT Token
     * @param username 用户名
     * @return 是否有效
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
