package com.secondbrain.service.impl;

import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketServiceImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendTaskUpdate(String userId, AsyncTaskResponse taskResponse) {
        try {
            messagingTemplate.convertAndSend("/topic/tasks/" + userId, taskResponse);
            log.debug("发送任务更新，userId：{}，taskNumber：{}", userId, taskResponse.getTaskId());
        } catch (Exception e) {
            log.error("发送任务更新失败，userId：{}", userId, e);
        }
    }

    @Override
    public void sendTaskProgress(String userId, String taskNumber, Integer progress) {
        try {
            AsyncTaskResponse response = new AsyncTaskResponse();
            response.setTaskId(taskNumber);
            response.setProgress(progress);
            response.setStatus("PROCESSING");
            
            messagingTemplate.convertAndSend("/topic/tasks/" + userId, response);
            log.debug("发送任务进度，userId：{}，taskNumber：{}，progress：{}", userId, taskNumber, progress);
        } catch (Exception e) {
            log.error("发送任务进度失败，userId：{}", userId, e);
        }
    }

    @Override
    public void sendTaskComplete(String userId, String taskNumber, Object result) {
        try {
            AsyncTaskResponse response = new AsyncTaskResponse();
            response.setTaskId(taskNumber);
            response.setStatus("COMPLETED");
            response.setProgress(100);
            response.setResult(result);
            
            messagingTemplate.convertAndSend("/topic/tasks/" + userId, response);
            log.info("发送任务完成通知，userId：{}，taskNumber：{}", userId, taskNumber);
        } catch (Exception e) {
            log.error("发送任务完成通知失败，userId：{}", userId, e);
        }
    }

    @Override
    public void sendTaskFailed(String userId, String taskNumber, String errorMessage) {
        try {
            AsyncTaskResponse response = new AsyncTaskResponse();
            response.setTaskId(taskNumber);
            response.setStatus("FAILED");
            response.setErrorMessage(errorMessage);
            
            messagingTemplate.convertAndSend("/topic/tasks/" + userId, response);
            log.error("发送任务失败通知，userId：{}，taskNumber：{}，error：{}", userId, taskNumber, errorMessage);
        } catch (Exception e) {
            log.error("发送任务失败通知失败，userId：{}", userId, e);
        }
    }
}