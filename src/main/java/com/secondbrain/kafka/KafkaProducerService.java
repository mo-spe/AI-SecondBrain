package com.secondbrain.kafka;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String CHAT_COLLECT_TOPIC = "chat-collect-topic";
    private static final String ASYNC_TASK_TOPIC = "async-task-topic";

    public void sendChatCollect(RawChatRecord chatRecord) {
        if (kafkaTemplate == null) {
            log.warn("Kafka未启用，跳过消息发送");
            return;
        }
        
        try {
            String message = JSON.toJSONString(chatRecord);
            kafkaTemplate.send(CHAT_COLLECT_TOPIC, message);
            log.info("对话采集消息已发送到Kafka，topic：{}，消息：{}", CHAT_COLLECT_TOPIC, message);
        } catch (Exception e) {
            log.error("发送对话采集消息到Kafka失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    public void sendAsyncTask(AsyncTaskRequest request) {
        if (kafkaTemplate == null) {
            log.warn("Kafka未启用，跳过消息发送");
            return;
        }
        
        try {
            String message = JSON.toJSONString(request);
            kafkaTemplate.send(ASYNC_TASK_TOPIC, message);
            log.info("异步任务消息已发送到Kafka，topic：{}，taskType：{}，userId：{}", 
                ASYNC_TASK_TOPIC, request.getTaskType(), request.getUserId());
        } catch (Exception e) {
            log.error("发送异步任务消息到Kafka失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
