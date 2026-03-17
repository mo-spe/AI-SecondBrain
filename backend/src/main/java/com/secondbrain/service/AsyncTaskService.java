package com.secondbrain.service;

import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.entity.AsyncTask;

public interface AsyncTaskService {

    AsyncTaskResponse createTask(String taskType, Long userId, Object parameters);

    AsyncTaskResponse getTaskStatus(String taskNumber);

    AsyncTask getTaskByNumber(String taskNumber);

    void processTask(AsyncTask task);

    void updateTaskProgress(String taskNumber, Integer progress);

    void completeTask(String taskNumber, Object result);

    void failTask(String taskNumber, String errorMessage);

    void updateTaskStatus(String taskNumber, String status, Integer progress, String result);
}