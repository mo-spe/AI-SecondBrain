package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建研究项目请求.
 *
 * @author AI
 */
@Getter
@Setter
public class CreateResearchProjectRequest {

    /**
     * 研究标题
     */
    @NotBlank(message = "研究标题不能为空")
    @Size(max = 300, message = "研究标题不能超过300个字符")
    private String title;

    /**
     * 研究目标描述
     */
    @NotBlank(message = "研究目标不能为空")
    private String goal;

    /**
     * 工作空间ID（可选，null 表示个人空间）
     */
    private Long workspaceId;
}
