package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.AsyncTask;
import com.secondbrain.entity.ResearchHistory;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.mapper.AsyncTaskMapper;
import com.secondbrain.service.AsyncTaskService;
import com.secondbrain.service.DeerFlowReportService;
import com.secondbrain.service.DeerFlowResearchService;
import com.secondbrain.service.ResearchHistoryService;
import com.secondbrain.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);

    @Autowired
    private AsyncTaskMapper asyncTaskMapper;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired(required = false)
    private WebSocketService webSocketService;

    @Lazy
    @Autowired
    private DeerFlowReportService deerFlowReportService;

    @Lazy
    @Autowired
    private DeerFlowResearchService deerFlowResearchService;

    @Lazy
    @Autowired
    private ResearchHistoryService researchHistoryService;

    @Override
    public AsyncTaskResponse createTask(String taskType, Long userId, Object parameters) {
        String taskNumber = taskType + "_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);

        AsyncTask task = new AsyncTask();
        task.setTaskNumber(taskNumber);
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setParameters(JSON.toJSONString(parameters));
        task.setCreateTime(LocalDateTime.now());
        task.setDeleted(0);

        asyncTaskMapper.insert(task);

        log.info("创建异步任务成功，taskNumber：{}，taskType：{}，userId：{}", taskNumber, taskType, userId);

        AsyncTaskRequest request = new AsyncTaskRequest();
        request.setTaskType(taskType);
        request.setUserId(userId);
        request.setParameters(parameters);
        request.setTaskId(taskNumber);

        kafkaProducerService.sendAsyncTask(request);

        return convertToResponse(task);
    }

    @Override
    public AsyncTaskResponse getTaskStatus(String taskNumber) {
        AsyncTask task = asyncTaskMapper.selectOne(
            new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskNumber, taskNumber)
        );

        if (task == null) {
            return null;
        }

        return convertToResponse(task);
    }

    @Override
    public AsyncTask getTaskByNumber(String taskNumber) {
        return asyncTaskMapper.selectOne(
            new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskNumber, taskNumber)
        );
    }

    @Override
    public void processTask(AsyncTask task) {
        try {
            log.info("开始处理异步任务，taskNumber：{}，taskType：{}", task.getTaskNumber(), task.getTaskType());

            task.setStatus("PROCESSING");
            task.setStartTime(LocalDateTime.now());
            asyncTaskMapper.updateById(task);

            Object result = executeTask(task);

            completeTask(task.getTaskNumber(), result);

        } catch (Exception e) {
            log.error("处理异步任务失败，taskNumber：{}", task.getTaskNumber(), e);
            failTask(task.getTaskNumber(), e.getMessage());
        }
    }

    private Object executeTask(AsyncTask task) {
        Map<String, Object> parameters = JSON.parseObject(task.getParameters(), Map.class);

        switch (task.getTaskType()) {
            case "LEARNING_REPORT":
                updateTaskProgress(task.getTaskNumber(), 20);
                String topic = (String) parameters.get("topic");
                String depth = (String) parameters.get("depth");
                String reportApiKey = (String) parameters.get("api_key");
                if (depth == null || depth.isEmpty()) {
                    depth = "medium";
                }
                updateTaskProgress(task.getTaskNumber(), 50);
                String report = deerFlowResearchService.generateDeepLearningReport(
                    JSON.toJSONString(parameters), topic, depth, reportApiKey
                );
                updateTaskProgress(task.getTaskNumber(), 100);
                return report;

            case "AI_RESEARCH":
                updateTaskProgress(task.getTaskNumber(), 20);
                String researchTopic = (String) parameters.get("topic");
                String researchApiKey = (String) parameters.get("api_key");
                updateTaskProgress(task.getTaskNumber(), 50);
                String researchResult = deerFlowResearchService.generateDeepLearningReport(
                    JSON.toJSONString(parameters), researchTopic, "deep", researchApiKey
                );
                updateTaskProgress(task.getTaskNumber(), 100);
                return researchResult;

            case "LEARNING_PATH":
                updateTaskProgress(task.getTaskNumber(), 20);
                String pathTopic = (String) parameters.get("topic");
                String currentLevel = (String) parameters.get("current_level");
                String targetLevel = (String) parameters.get("target_level");
                String pathApiKey = (String) parameters.get("api_key");
                updateTaskProgress(task.getTaskNumber(), 50);
                String pathResult = deerFlowResearchService.generateLearningPath(
                    pathTopic, currentLevel, targetLevel, pathApiKey
                );
                updateTaskProgress(task.getTaskNumber(), 100);
                return pathResult;

            case "KNOWLEDGE_BLIND_SPOT":
                updateTaskProgress(task.getTaskNumber(), 20);
                String blindSpotTopic = (String) parameters.get("topic");
                java.util.List<String> userKnowledge = (java.util.List<String>) parameters.get("user_knowledge");
                String blindSpotApiKey = (String) parameters.get("api_key");
                updateTaskProgress(task.getTaskNumber(), 50);
                String blindSpotResult = deerFlowResearchService.researchKnowledgeGap(
                    userKnowledge, blindSpotTopic, blindSpotApiKey
                );
                updateTaskProgress(task.getTaskNumber(), 100);
                return blindSpotResult;

            default:
                throw new RuntimeException("未知的任务类型：" + task.getTaskType());
        }
    }

    @Override
    public void updateTaskProgress(String taskNumber, Integer progress) {
        AsyncTask task = getTaskByNumber(taskNumber);
        if (task != null) {
            task.setProgress(progress);
            asyncTaskMapper.updateById(task);
            log.info("更新任务进度，taskNumber：{}，progress：{}", taskNumber, progress);
            
            if (webSocketService != null) {
                webSocketService.sendTaskProgress(task.getUserId().toString(), taskNumber, progress);
            }
        }
    }

    @Override
    public void completeTask(String taskNumber, Object result) {
        AsyncTask task = getTaskByNumber(taskNumber);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setResult(JSON.toJSONString(result));
            task.setCompleteTime(LocalDateTime.now());
            asyncTaskMapper.updateById(task);
            log.info("任务完成，taskNumber：{}", taskNumber);
            
            if (webSocketService != null) {
                webSocketService.sendTaskComplete(task.getUserId().toString(), taskNumber, result);
            }
            
            saveToResearchHistory(task, result);
        }
    }
    
    private void saveToResearchHistory(AsyncTask task, Object result) {
        try {
            Map<String, Object> parameters = JSON.parseObject(task.getParameters(), Map.class);
            
            ResearchHistoryRequest historyRequest = new ResearchHistoryRequest();
            historyRequest.setType(task.getTaskType());
            historyRequest.setContent(JSON.toJSONString(result));
            
            String topic = null;
            String learningData = null;
            String depth = null;
            
            if (parameters.containsKey("topic")) {
                topic = (String) parameters.get("topic");
                historyRequest.setTopic(topic);
            }
            if (parameters.containsKey("learningData")) {
                learningData = (String) parameters.get("learningData");
                if (topic == null) {
                    historyRequest.setTopic(learningData);
                }
            }
            if (parameters.containsKey("depth")) {
                depth = (String) parameters.get("depth");
                historyRequest.setDepth(depth);
            }
            
            String currentLevel = null;
            String targetLevel = null;
            
            if (parameters.containsKey("current_level")) {
                currentLevel = (String) parameters.get("current_level");
                historyRequest.setCurrentLevel(currentLevel);
            }
            if (parameters.containsKey("target_level")) {
                targetLevel = (String) parameters.get("target_level");
                historyRequest.setTargetLevel(targetLevel);
            }
            
            String type = task.getTaskType();
            
            switch (type) {
                case "LEARNING_PATH":
                    historyRequest.setType("path");
                    break;
                case "LEARNING_REPORT":
                    historyRequest.setType("report");
                    break;
                case "KNOWLEDGE_BLIND_SPOT":
                    historyRequest.setType("gaps");
                    break;
                case "AI_RESEARCH":
                    historyRequest.setType("research");
                    break;
                default:
                    historyRequest.setType("other");
            }
            
            researchHistoryService.save(historyRequest, task.getUserId());
            
            log.info("异步任务已保存到研究历史，taskNumber：{}，type：{}，topic：{}，currentLevel：{}，targetLevel：{}", 
                task.getTaskNumber(), task.getTaskType(), historyRequest.getTopic(), currentLevel, targetLevel);
                
        } catch (Exception e) {
            log.error("保存异步任务到研究历史失败，taskNumber：{}", task.getTaskNumber(), e);
        }
    }

    @Override
    public void failTask(String taskNumber, String errorMessage) {
        AsyncTask task = getTaskByNumber(taskNumber);
        if (task != null) {
            task.setStatus("FAILED");
            task.setErrorMessage(errorMessage);
            task.setCompleteTime(LocalDateTime.now());
            asyncTaskMapper.updateById(task);
            log.error("任务失败，taskNumber：{}，errorMessage：{}", taskNumber, errorMessage);
            
            if (webSocketService != null) {
                webSocketService.sendTaskFailed(task.getUserId().toString(), taskNumber, errorMessage);
            }
        }
    }

    @Override
    public void updateTaskStatus(String taskNumber, String status, Integer progress, String result) {
        AsyncTask task = getTaskByNumber(taskNumber);
        if (task != null) {
            task.setStatus(status);
            if (progress != null) {
                task.setProgress(progress);
            }
            if (result != null) {
                task.setResult(result);
            }
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setCompleteTime(LocalDateTime.now());
            }
            asyncTaskMapper.updateById(task);
            log.info("更新任务状态，taskNumber：{}，status：{}，progress：{}", taskNumber, status, progress);
            
            if (webSocketService != null) {
                AsyncTaskResponse response = convertToResponse(task);
                webSocketService.sendTaskUpdate(task.getUserId().toString(), response);
            }
        }
    }

    private AsyncTaskResponse convertToResponse(AsyncTask task) {
        AsyncTaskResponse response = new AsyncTaskResponse();
        response.setTaskId(task.getTaskNumber());
        response.setStatus(task.getStatus());
        response.setTaskType(task.getTaskType());
        response.setCreateTime(task.getCreateTime().toString());
        response.setProgress(task.getProgress());
        
        if (task.getCompleteTime() != null) {
            response.setCompleteTime(task.getCompleteTime().toString());
        }
        
        if (task.getResult() != null) {
            try {
                response.setResult(JSON.parse(task.getResult()));
            } catch (Exception e) {
                response.setResult(task.getResult());
            }
        }
        
        if (task.getErrorMessage() != null) {
            response.setErrorMessage(task.getErrorMessage());
        }
        
        return response;
    }
}