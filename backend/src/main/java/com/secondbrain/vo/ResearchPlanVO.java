package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究计划视图对象.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchPlanVO {

    /**
     * 计划ID
     */
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 计划版本号
     */
    private Integer version;

    /**
     * 复杂度评估
     */
    private String complexity;

    /**
     * 复杂度中文描述
     */
    private String complexityLabel;

    /**
     * Agent 执行链路
     */
    private String agentChain;

    /**
     * Task 列表 JSON
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
     * 创建者
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
