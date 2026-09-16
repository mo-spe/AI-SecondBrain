package com.secondbrain.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.service.KnowledgeCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer service.
 * Listens to the chat-collect queue and handles knowledge extraction.
 */
@Service
@ConditionalOnProperty(prefix = "spring.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final KnowledgeCaptureService knowledgeCaptureService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(KnowledgeCaptureService knowledgeCaptureService,
                                ObjectMapper objectMapper) {
        this.knowledgeCaptureService = knowledgeCaptureService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes chat-collect messages.
     * Kafka is configured with StringDeserializer; messages arrive as JSON strings and are deserialized manually.
     * When extractKnowledge=true, AI extracts knowledge points and writes them to pending_knowledge for user confirmation.
     */
    @KafkaListener(topics = "chat-collect", groupId = "chat-collect-group")
    public void consumeChatCollect(String messageJson) {
        log.info("Received chat-collect message, length={}", messageJson.length());

        try {
            ChatCollectMessage message = objectMapper.readValue(messageJson, ChatCollectMessage.class);
            RawChatRecord record = message.getRecord();
            log.info("Parsed message userId={} workspaceId={} extractKnowledge={}",
                    record.getUserId(), message.getWorkspaceId(), message.getExtractKnowledge());

            // Record is already persisted in ChatServiceImpl; only knowledge extraction runs here
            if (record.getWorkspaceId() == null && message.getWorkspaceId() != null) {
                record.setWorkspaceId(message.getWorkspaceId());
            }

            if (Boolean.TRUE.equals(message.getExtractKnowledge())) {
                int count = knowledgeCaptureService.extractKnowledge(record);
                log.info("Knowledge extraction completed recordId={} extracted={}", record.getId(), count);
            }
        } catch (Exception e) {
            log.error("Failed to process chat-collect message", e);
        }
    }
}