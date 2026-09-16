package com.secondbrain.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka-backed producer implementation.
 * Active only when {@code spring.kafka.enabled=true}.
 */
@Service
@ConditionalOnProperty(prefix = "spring.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
public class KafkaProducerService implements KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendChatCollect(ChatCollectMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            log.info("Sending chat-collect message to Kafka, userId={} workspaceId={} extractKnowledge={} generateCards={}",
                    message.getRecord().getUserId(), message.getWorkspaceId(),
                    message.getExtractKnowledge(), message.getGenerateCards());
            kafkaTemplate.send("chat-collect", json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed recordId={}", message.getRecord().getId(), ex);
                        } else {
                            log.info("Kafka send succeeded recordId={} offset={}",
                                    message.getRecord().getId(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize chat-collect message", e);
        }
    }

    @Override
    @Deprecated
    public void sendChatCollect(RawChatRecord record) {
        ChatCollectMessage message = new ChatCollectMessage();
        message.setRecord(record);
        message.setWorkspaceId(record.getWorkspaceId());
        message.setExtractKnowledge(true);
        message.setGenerateCards(false);
        sendChatCollect(message);
    }

    @Override
    public void sendMessage(String topic, Object message) {
        try {
            String json = message instanceof String ? (String) message : objectMapper.writeValueAsString(message);
            log.info("Sending message to Kafka topic: {}", topic);
            kafkaTemplate.send(topic, json);
            log.info("Message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send message to Kafka, topic={}", topic, e);
        }
    }

    @Override
    public void sendAsyncTask(AsyncTaskRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.info("Sending async task to Kafka, taskId={} type={}", request.getTaskId(), request.getTaskType());
            kafkaTemplate.send("async-tasks", json);
            log.info("Async task sent successfully");
        } catch (Exception e) {
            log.error("Failed to send async task, taskId={}", request.getTaskId(), e);
        }
    }
}