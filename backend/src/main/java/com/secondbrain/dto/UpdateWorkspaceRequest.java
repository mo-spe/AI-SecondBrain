package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 更新工作区请求DTO. <p>用于工作区信息更新的请求参数封装</p> */
@Getter
@Setter
public class UpdateWorkspaceRequest {

    /**
     * 工作区名称
     */
    @NotBlank(message = "工作区名称不能为空")
    @Size(max = 100, message = "工作区名称不能超过100个字符")
    private String name;

    /**
     * 工作区描述
     */
    @Size(max = 500, message = "工作区描述不能超过500个字符")
    private String description;
}
