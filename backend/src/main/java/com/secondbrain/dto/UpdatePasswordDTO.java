package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 修改密码DTO. <p>用于接收用户修改密码请求</p> */
@Getter
@Setter
public class UpdatePasswordDTO {

    /**
     * 原密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
