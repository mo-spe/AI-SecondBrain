package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * SSE 事件推送的研究步骤事件.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchStepEvent {

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 步骤类型
     */
    private String stepType;

    /**
     * 步骤描述
     */
    private String title;

    /**
     * 步骤内容
     */
    private String content;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 状态
     */
    private String status;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;
}
