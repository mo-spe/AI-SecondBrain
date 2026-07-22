package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 异步任务请求DTO.
 */
@Getter
@Setter
public class AsyncTaskRequest {

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务参数
     */
    private Object parameters;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 回调URL
     */
    private String callbackUrl;
}
