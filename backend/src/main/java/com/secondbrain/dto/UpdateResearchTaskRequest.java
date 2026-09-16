package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新研究任务请求.
 *
 * <p>所有字段均为可选，仅更新非 null 字段。</p>
 *
 * @author AI
 */
@Getter
@Setter
public class UpdateResearchTaskRequest {

    /**
     * 任务标题
     */
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
     * 任务状态
     */
    private String status;

    /**
     * 依赖的前置任务ID
     */
    private Long dependsOn;

    /**
     * 是否需要外部搜索
     */
    private Boolean requiresExternalSearch;

    /**
     * 任务结果摘要
     */
    private String resultSummary;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
