package com.secondbrain.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.service.KnowledgeCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/** Kafka消费者服务. <p>监听聊天采集队列并处理知识提取</p> */
@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final KnowledgeCaptureService knowledgeCaptureService;
    private final ObjectMapper objectMapper;

    /**
     * 构造器注入依赖.
     */
    public KafkaConsumerService(KnowledgeCaptureService knowledgeCaptureService,
                                ObjectMapper objectMapper) {
        this.knowledgeCaptureService = knowledgeCaptureService;
        this.objectMapper = objectMapper;
    }

    /**
     * 消费聊天采集消息.
     * <p>Kafka 配置使用 StringDeserializer，消息以 JSON 字符串传入，需手动反序列化</p>
     * <p>如果 extractKnowledge=true，AI 提取知识点后写入 pending_knowledge 表等待用户确认</p>
     */
    @KafkaListener(topics = "chat-collect", groupId = "chat-collect-group")
    public void consumeChatCollect(String messageJson) {
        log.info("收到聊天采集消息，长度={}", messageJson.length());

        try {
            ChatCollectMessage message = objectMapper.readValue(messageJson, ChatCollectMessage.class);
            RawChatRecord record = message.getRecord();
            log.info("解析消息 userId={} workspaceId={} extractKnowledge={}",
                    record.getUserId(), message.getWorkspaceId(), message.getExtractKnowledge());

            // 记录已在 ChatServiceImpl 中持久化，此处仅处理知识提取
            if (record.getWorkspaceId() == null && message.getWorkspaceId() != null) {
                record.setWorkspaceId(message.getWorkspaceId());
            }

            if (Boolean.TRUE.equals(message.getExtractKnowledge())) {
                int count = knowledgeCaptureService.extractKnowledge(record, message.getGenerateCards());
                log.info("知识提取完成 recordId={} extracted={} needReview={}",
                        record.getId(), count, message.getGenerateCards());
            }
        } catch (Exception e) {
            log.error("处理聊天采集消息失败", e);
        }
    }
}
