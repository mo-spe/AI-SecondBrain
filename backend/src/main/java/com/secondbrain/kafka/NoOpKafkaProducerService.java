package com.secondbrain.kafka;

import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * No-op producer implementation.
 * Active only when {@code spring.kafka.enabled=false}.
 */
@Service
@ConditionalOnProperty(prefix = "spring.kafka", name = "enabled", havingValue = "false")
public class NoOpKafkaProducerService implements KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(NoOpKafkaProducerService.class);

    @Override
    public void sendChatCollect(ChatCollectMessage message) {
        log.debug("Kafka disabled, skipping chat-collect send: userId={} workspaceId={}",
                message.getRecord().getUserId(), message.getWorkspaceId());
    }

    @Override
    @Deprecated
    public void sendChatCollect(RawChatRecord record) {
        log.debug("Kafka disabled, skipping chat-collect send: recordId={}", record.getId());
    }

    @Override
    public void sendMessage(String topic, Object message) {
        log.debug("Kafka disabled, skipping send to topic: {}", topic);
    }

    @Override
    public void sendAsyncTask(AsyncTaskRequest request) {
        log.debug("Kafka disabled, skipping async task send: taskId={} taskType={}",
                request.getTaskId(), request.getTaskType());
    }
}