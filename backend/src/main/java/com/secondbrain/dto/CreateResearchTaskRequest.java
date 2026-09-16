package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建研究任务请求.
 *
 * @author AI
 */
@Getter
@Setter
public class CreateResearchTaskRequest {

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 300, message = "任务标题不能超过300个字符")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 该任务要回答的研究问题
     */
    private String question;

    /**
     * 依赖的前置任务ID（可选）
     */
    private Long dependsOn;

    /**
     * 是否需要外部搜索
     */
    private Boolean requiresExternalSearch;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
