package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究步骤实体（Agent 执行日志，append-only）.
 *
 * <p>记录每个 Agent 执行过程中的思考、工具调用和结果。
 * 步骤只增不改，用于前端实时展示和事后审计。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_step")
public class ResearchStep {

    /**
     * 步骤ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属任务ID
     */
    private Long taskId;

    /**
     * 所属执行会话ID
     */
    private Long executionId;

    /**
     * Agent 标识
     */
    private String agentName;

    /**
     * 步骤类型：THINKING/TOOL_CALL/TOOL_RESULT/LLM_CALL/PROCESSING
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
     * 工具入参（JSON）
     */
    private String toolInput;

    /**
     * 工具输出
     */
    private String toolOutput;

    /**
     * Token 消耗（JSON: {"prompt": N, "completion": N}）
     */
    private String tokenUsage;

    /**
     * 状态：RUNNING/COMPLETED/FAILED/SKIPPED
     */
    private String status;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
