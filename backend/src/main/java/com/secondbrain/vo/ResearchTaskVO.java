package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 研究任务视图对象.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchTaskVO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 所属计划ID
     */
    private Long planId;

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
     * 状态中文描述
     */
    private String statusLabel;

    /**
     * 依赖的前置任务ID
     */
    private Long dependsOn;

    /**
     * 依赖的前置任务标题
     */
    private String dependsOnTitle;

    /**
     * 关联的异步任务ID
     */
    private Long asyncTaskId;

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

    /**
     * 是否为可执行任务（依赖已满足）
     */
    private Boolean isReady;

    /**
     * 依赖该任务的后置任务ID列表
     */
    private List<Long> dependentTaskIds;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
