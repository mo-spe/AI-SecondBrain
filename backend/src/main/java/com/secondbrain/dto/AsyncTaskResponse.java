package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "异步任务响应")
public class AsyncTaskResponse {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "完成时间")
    private String completeTime;

    @Schema(description = "进度")
    private Integer progress;

    @Schema(description = "结果")
    private Object result;

    @Schema(description = "错误信息")
    private String errorMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(String completeTime) {
        this.completeTime = completeTime;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}