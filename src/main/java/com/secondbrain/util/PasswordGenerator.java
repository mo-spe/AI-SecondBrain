package com.secondbrain.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "123456";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt 哈希值: " + hashedPassword);
        
        // 验证密码
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("验证结果: " + matches);
    }
}
