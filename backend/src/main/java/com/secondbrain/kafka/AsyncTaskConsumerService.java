package com.secondbrain.kafka;

import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.entity.AsyncTask;
import com.secondbrain.service.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Async task consumer service.
 * Listens to the async-tasks queue and delegates processing to AsyncTaskService.
 */
@Service
@ConditionalOnProperty(prefix = "spring.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AsyncTaskConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskConsumerService.class);

    private final AsyncTaskService asyncTaskService;

    public AsyncTaskConsumerService(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * Consumes async task messages from Kafka.
     *
     * @param taskRequest async task request
     */
    @KafkaListener(topics = "async-tasks", groupId = "async-task-group")
    public void consumeAsyncTask(AsyncTaskRequest taskRequest) {
        log.info("Received async task, taskId={} type={}", taskRequest.getTaskId(), taskRequest.getTaskType());

        try {
            AsyncTask task = asyncTaskService.getTaskByNumber(taskRequest.getTaskId());
            if (task != null) {
                asyncTaskService.processTask(task);
            }
            log.info("Async task completed, taskId={}", taskRequest.getTaskId());
        } catch (Exception e) {
            log.error("Async task failed, taskId={}", taskRequest.getTaskId(), e);
        }
    }
}