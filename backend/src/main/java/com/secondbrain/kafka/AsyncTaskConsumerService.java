package com.secondbrain.kafka;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.entity.AsyncTask;
import com.secondbrain.mapper.AsyncTaskMapper;
import com.secondbrain.service.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AsyncTaskConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskConsumerService.class);

    private static final String ASYNC_TASK_TOPIC = "async-task-topic";

    @Autowired
    private AsyncTaskService asyncTaskService;

    @KafkaListener(topics = ASYNC_TASK_TOPIC, groupId = "async-task-group")
    public void handleAsyncTask(String message) {
        try {
            log.info("收到异步任务消息：{}", message);

            AsyncTaskRequest request = JSON.parseObject(message, AsyncTaskRequest.class);
            
            AsyncTask task = asyncTaskService.getTaskByNumber(request.getTaskId());
            if (task == null) {
                log.warn("任务不存在，taskNumber：{}", request.getTaskId());
                return;
            }

            asyncTaskService.processTask(task);

        } catch (Exception e) {
            log.error("处理异步任务失败", e);
        }
    }
}