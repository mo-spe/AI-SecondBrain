package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究项目实体.
 *
 * <p>管理 AI 研究项目的完整生命周期，从草稿到完成归档。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_project")
public class ResearchProject {

    /**
     * 项目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作空间ID（NULL=个人空间）
     */
    private Long workspaceId;

    /**
     * 研究标题
     */
    private String title;

    /**
     * 研究目标描述
     */
    private String goal;

    /**
     * 项目状态：DRAFT/PLANNING/RESEARCHING/REVIEWING/SYNTHESIZING/COMPLETED/ARCHIVED/FAILED/PAUSED
     */
    private String status;

    /**
     * 复杂度评估：SIMPLE/STANDARD/DEEP
     */
    private String complexity;

    /**
     * 实际执行的 Agent 链路（逗号分隔）
     */
    private String agentWorkflow;

    /**
     * 最大 Agent 迭代轮数
     */
    private Integer maxIterations;

    /**
     * 当前迭代轮数
     */
    private Integer currentIteration;

    /**
     * Research Plan 结构化数据（Planner 输出 JSON）
     */
    private String planJson;

    /**
     * 研究结论摘要
     */
    private String resultSummary;

    /**
     * 完整研究报告（Markdown）
     */
    private String resultReport;

    /**
     * AgentContext 序列化快照（用于暂停恢复）
     */
    private String contextSnapshot;

    /**
     * 幂等键（执行操作去重）
     */
    private String idempotencyKey;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 首次执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 最近暂停时间
     */
    private LocalDateTime pausedAt;

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

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
