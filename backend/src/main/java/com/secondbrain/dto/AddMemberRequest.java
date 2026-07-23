package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 邀请成员请求DTO. <p>用于工作区成员邀请入参封装</p> */
@Getter
@Setter
public class AddMemberRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 角色（admin/editor/viewer）
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}
