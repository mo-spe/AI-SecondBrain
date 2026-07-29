package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究步骤视图对象.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchStepVO {

    /**
     * 步骤ID
     */
    private Long id;

    /**
     * 所属任务ID
     */
    private Long taskId;

    /**
     * Agent 标识
     */
    private String agentName;

    /**
     * 步骤类型
     */
    private String stepType;

    /**
     * 步骤类型中文描述
     */
    private String stepTypeLabel;

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
     * 工具入参
     */
    private String toolInput;

    /**
     * 工具输出
     */
    private String toolOutput;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态中文描述
     */
    private String statusLabel;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 步骤序号
     */
    private Integer sortOrder;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
