package com.secondbrain.kafka;

import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.service.AsyncTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** 异步任务消费者服务. <p>监听Kafka异步任务队列并执行任务处理</p> */
@Service
public class AsyncTaskConsumerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskConsumerService.class);

    private final AsyncTaskService asyncTaskService;

    /**
     * 构造器注入异步任务服务.
     *
     * @param asyncTaskService 异步任务服务
     */
    public AsyncTaskConsumerService(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 消费异步任务消息.
     *
     * @param taskRequest 异步任务请求
     * @return void
     */
    @KafkaListener(topics = "async-tasks", groupId = "async-task-group")
    public void consumeAsyncTask(AsyncTaskRequest taskRequest) {
        log.info("收到异步任务，taskId：{}，type：{}", taskRequest.getTaskId(), taskRequest.getTaskType());
        
        try {
            com.secondbrain.entity.AsyncTask task = asyncTaskService.getTaskByNumber(taskRequest.getTaskId());
            if (task != null) {
                asyncTaskService.processTask(task);
            }
            log.info("异步任务执行成功，taskId：{}", taskRequest.getTaskId());
        } catch (Exception e) {
            log.error("异步任务执行失败，taskId：{}", taskRequest.getTaskId(), e);
        }
    }
}
