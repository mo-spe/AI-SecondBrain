package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 更新成员角色请求DTO. <p>用于更新工作区成员角色的请求参数</p> */
@Getter
@Setter
public class UpdateMemberRoleRequest {

    /**
     * 角色（admin/editor/viewer）
     */
    @NotBlank(message = "角色不能为空")
    private String role;
}
