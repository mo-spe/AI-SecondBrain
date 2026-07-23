package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 异步任务响应DTO. <p>用于异步任务状态查询返回结果</p> */
@Getter
@Setter
public class AsyncTaskResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态（PENDING/PROCESSING/COMPLETED/FAILED）
     */
    private String status;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 完成时间
     */
    private String completeTime;

    /**
     * 进度（0-100）
     */
    private Integer progress;

    /**
     * 任务结果
     */
    private Object result;

    /**
     * 错误信息
     */
    private String errorMessage;
}
