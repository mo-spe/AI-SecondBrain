package com.secondbrain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncTaskManager {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncTaskManager.class);
    
    private final Map<String, AsyncTaskInfo> taskStore = new ConcurrentHashMap<>();
    
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
    
    public AsyncTaskInfo getTask(String taskId) {
        return taskStore.get(taskId);
    }
    
    public static class AsyncTaskInfo {
        private String taskId;
        private String taskType;
        private Long userId;
        private String status;
        private int progress;
        private String result;
        private long createTime;
        private long updateTime;
        
        public String getTaskId() {
            return taskId;
        }
        
        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }
        
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
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public void setProgress(int progress) {
            this.progress = progress;
        }
        
        public String getResult() {
            return result;
        }
        
        public void setResult(String result) {
            this.result = result;
        }
        
        public long getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }
        
        public long getUpdateTime() {
            return updateTime;
        }
        
        public void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }
    }
}