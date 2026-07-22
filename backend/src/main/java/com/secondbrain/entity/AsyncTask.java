package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 异步任务实体类.
 * <p>存储异步任务的状态、进度、参数及执行结果</p>
 */
@Getter
@Setter
@TableName("async_task")
public class AsyncTask {

    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务编号（唯一标识）
     */
    private String taskNumber;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务类型（LEARNING_REPORT/AI_RESEARCH/LEARNING_PATH/KNOWLEDGE_BLIND_SPOT）
     */
    private String taskType;

    /**
     * 任务状态（PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，FAILED-失败）
     */
    private String status;

    /**
     * 进度（0-100）
     */
    private Integer progress;

    /**
     * 任务参数（JSON格式）
     */
    private String parameters;

    /**
     * 任务结果（JSON格式）
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 删除标记（0-未删除，1-已删除）
     */
    private Integer deleted;
}
