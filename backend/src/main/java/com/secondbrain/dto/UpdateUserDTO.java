package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新用户信息请求DTO.
 */
@Getter
@Setter
public class UpdateUserDTO {

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
     * API Key（用于调用外部AI服务）
     */
    private String apiKey;
}
