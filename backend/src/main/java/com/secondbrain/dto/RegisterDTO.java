package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 注册请求DTO. <p>用于接收用户注册信息</p> */
@Getter
@Setter
public class RegisterDTO {

    /**
     * 用户名（3-20个字符）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度在3到20个字符")
    private String username;

    /**
     * 密码（至少6位）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;
}
