package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究项目视图对象.
 *
 * <p>用于前端展示，隐藏内部字段（userId、idempotencyKey、contextSnapshot 等）。</p>
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchProjectVO {

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 研究标题
     */
    private String title;

    /**
     * 研究目标描述
     */
    private String goal;

    /**
     * 项目状态
     */
    private String status;

    /**
     * 状态中文描述
     */
    private String statusLabel;

    /**
     * 复杂度评估
     */
    private String complexity;

    /**
     * 实际执行的 Agent 链路
     */
    private String agentWorkflow;

    /**
     * 最大迭代轮数
     */
    private Integer maxIterations;

    /**
     * 当前迭代轮数
     */
    private Integer currentIteration;

    /**
     * 研究结论摘要
     */
    private String resultSummary;

    /**
     * 任务总数
     */
    private Integer taskCount;

    /**
     * 已完成任务数
     */
    private Integer completedTaskCount;

    /**
     * 工作空间ID（null 表示个人空间）
     */
    private Long workspaceId;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
