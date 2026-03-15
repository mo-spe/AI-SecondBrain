package com.secondbrain.service;

import com.secondbrain.dto.AsyncTaskResponse;

public interface WebSocketService {

    void sendTaskUpdate(String userId, AsyncTaskResponse taskResponse);

    void sendTaskProgress(String userId, String taskNumber, Integer progress);

    void sendTaskComplete(String userId, String taskNumber, Object result);

    void sendTaskFailed(String userId, String taskNumber, String errorMessage);
}