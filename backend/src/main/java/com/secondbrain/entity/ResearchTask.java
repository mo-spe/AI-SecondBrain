package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究任务实体.
 *
 * <p>一个研究项目包含多个研究任务，任务之间可以有依赖关系。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_task")
public class ResearchTask {

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
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
     * 任务状态：PENDING/RUNNING/COMPLETED/FAILED/SKIPPED/WAITING_USER
     */
    private String status;

    /**
     * 依赖的前置任务ID
     */
    private Long dependsOn;

    /**
     * 关联的异步任务ID
     */
    private Long asyncTaskId;

    /**
     * 是否需要外部搜索
     */
    private Integer requiresExternalSearch;

    /**
     * 任务结果摘要
     */
    private String resultSummary;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
