package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "异步任务请求")
public class AsyncTaskRequest {

    @Schema(description = "任务类型", required = true)
    private String taskType;

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "任务参数")
    private Object parameters;

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "回调URL")
    private String callbackUrl;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Object getParameters() {
        return parameters;
    }

    public void setParameters(Object parameters) {
        this.parameters = parameters;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}