package com.secondbrain.service;

import com.secondbrain.dto.AsyncTaskResponse;

/**
 * WebSocket服务接口.
 * <p>提供实时消息推送功能</p>
 */
public interface WebSocketService {

    /**
     * 发送任务更新.
     *
     * @param userId 用户ID
     * @param taskResponse 任务响应
     */
    void sendTaskUpdate(String userId, AsyncTaskResponse taskResponse);

    /**
     * 发送任务进度.
     *
     * @param userId 用户ID
     * @param taskNumber 任务编号
     * @param progress 进度
     */
    void sendTaskProgress(String userId, String taskNumber, Integer progress);

    /**
     * 发送任务完成消息.
     *
     * @param userId 用户ID
     * @param taskNumber 任务编号
     * @param result 任务结果
     */
    void sendTaskComplete(String userId, String taskNumber, Object result);

    /**
     * 发送任务失败消息.
     *
     * @param userId 用户ID
     * @param taskNumber 任务编号
     * @param errorMessage 错误信息
     */
    void sendTaskFailed(String userId, String taskNumber, String errorMessage);
}
