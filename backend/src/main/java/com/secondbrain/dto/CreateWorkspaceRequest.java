package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 创建工作区请求DTO. <p>用于工作区创建请求参数封装</p> */
@Getter
@Setter
public class CreateWorkspaceRequest {

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
