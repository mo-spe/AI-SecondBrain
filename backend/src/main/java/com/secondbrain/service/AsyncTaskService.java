package com.secondbrain.service;

import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.entity.AsyncTask;

/**
 * 异步任务服务接口.
 * <p>提供异步任务的创建、查询、更新等功能</p>
 */
public interface AsyncTaskService {

    /**
     * 创建异步任务.
     *
     * @param taskType 任务类型
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param parameters 任务参数
     * @return 任务响应
     */
    AsyncTaskResponse createTask(String taskType, Long userId, Long workspaceId, Object parameters);

    /**
     * 查询任务状态.
     *
     * @param taskNumber 任务编号
     * @return 任务响应
     */
    AsyncTaskResponse getTaskStatus(String taskNumber);

    /**
     * 根据编号获取任务实体.
     *
     * @param taskNumber 任务编号
     * @return 任务实体
     */
    AsyncTask getTaskByNumber(String taskNumber);

    /**
     * 处理任务.
     *
     * @param task 任务实体
     */
    void processTask(AsyncTask task);

    /**
     * 更新任务进度.
     *
     * @param taskNumber 任务编号
     * @param progress 进度（0-100）
     */
    void updateTaskProgress(String taskNumber, Integer progress);

    /**
     * 完成任务.
     *
     * @param taskNumber 任务编号
     * @param result 任务结果
     */
    void completeTask(String taskNumber, Object result);

    /**
     * 任务失败.
     *
     * @param taskNumber 任务编号
     * @param errorMessage 错误信息
     */
    void failTask(String taskNumber, String errorMessage);

    /**
     * 更新任务状态.
     *
     * @param taskNumber 任务编号
     * @param status 状态
     * @param progress 进度
     * @param result 结果
     */
    void updateTaskStatus(String taskNumber, String status, Integer progress, String result);
}
