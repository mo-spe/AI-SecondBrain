package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 登录响应DTO. <p>用于返回登录结果及用户信息</p> */
@Getter
@Setter
public class LoginResponseDTO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类.
     */
    @Getter
    @Setter
    public static class UserInfo {

        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户名
         */
        private String username;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 个人简介
         */
        private String bio;

        /**
         * 平台角色
         */
        private String role;
    }
}
