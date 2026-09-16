package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究计划实体.
 *
 * <p>Planner Agent 输出的结构化研究计划，包含复杂度评估、Agent 执行链路和任务列表。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_plan")
public class ResearchPlan {

    /**
     * 计划ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 计划版本号（每次重新规划递增）
     */
    private Integer version;

    /**
     * 复杂度评估：SIMPLE/STANDARD/DEEP
     */
    private String complexity;

    /**
     * Agent 执行链路（逗号分隔的 Agent 名称）
     */
    private String agentChain;

    /**
     * Task 列表 JSON：[{title, description, requiresExternalSearch, dependsOn}]
     */
    private String tasksJson;

    /**
     * Planner 推理过程
     */
    private String rationale;

    /**
     * 预估 token 消耗
     */
    private Integer estimatedTokens;

    /**
     * 创建者：PLANNER_AGENT / USER
     */
    private String createdBy;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
