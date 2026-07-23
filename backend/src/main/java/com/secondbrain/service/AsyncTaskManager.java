package com.secondbrain.service;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 异步任务管理器. <p>提供异步任务的内存存储、状态更新及查询功能</p> */
@Service
public class AsyncTaskManager {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskManager.class);

    private final Map<String, AsyncTaskInfo> taskStore = new ConcurrentHashMap<>();

    /**
     * 创建异步任务.
     *
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @param userId 用户ID
     * @return void
     */
    public void createTask(String taskId, String taskType, Long userId) {
        AsyncTaskInfo taskInfo = new AsyncTaskInfo();
        taskInfo.setTaskId(taskId);
        taskInfo.setTaskType(taskType);
        taskInfo.setUserId(userId);
        taskInfo.setStatus("PENDING");
        taskInfo.setProgress(0);
        taskInfo.setCreateTime(System.currentTimeMillis());

        taskStore.put(taskId, taskInfo);
        log.info("创建异步任务，taskNumber：{}，类型：{}，用户ID：{}", taskId, taskType, userId);
    }

    /**
     * 更新任务状态.
     *
     * @param taskId 任务ID
     * @param status 任务状态
     * @param progress 任务进度
     * @param result 任务结果
     * @return void
     */
    public void updateTaskStatus(String taskId, String status, int progress, String result) {
        AsyncTaskInfo taskInfo = taskStore.get(taskId);
        if (taskInfo != null) {
            taskInfo.setStatus(status);
            taskInfo.setProgress(progress);
            if (result != null) {
                taskInfo.setResult(result);
            }
            taskInfo.setUpdateTime(System.currentTimeMillis());
            log.info("更新任务状态，taskNumber：{}，状态：{}，进度：{}%", taskId, status, progress);
        }
    }

    /**
     * 根据任务ID获取任务信息.
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    public AsyncTaskInfo getTask(String taskId) {
        return taskStore.get(taskId);
    }

    /** 异步任务信息. <p>封装异步任务的状态、进度、结果等元数据</p> */
    @Getter
    @Setter
    public static class AsyncTaskInfo {
        /** 任务ID */
        private String taskId;
        /** 任务类型 */
        private String taskType;
        /** 用户ID */
        private Long userId;
        /** 任务状态 */
        private String status;
        /** 任务进度 */
        private int progress;
        /** 任务结果 */
        private String result;
        /** 创建时间 */
        private long createTime;
        /** 更新时间 */
        private long updateTime;
    }
}