package com.secondbrain.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码生成工具类.
 * <p>提供密码的BCrypt哈希编码和匹配验证功能</p>
 */
public final class PasswordGenerator {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordGenerator() {
        // 工具类禁止实例化
    }

    /**
     * 对原始密码进行BCrypt哈希编码.
     *
     * @param rawPassword 原始密码
     * @return BCrypt哈希后的密码
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证原始密码是否与BCrypt哈希匹配.
     *
     * @param rawPassword    原始密码
     * @param hashedPassword BCrypt哈希值
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}
