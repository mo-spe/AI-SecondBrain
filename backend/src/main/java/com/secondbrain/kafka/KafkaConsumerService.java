package com.secondbrain.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.service.KnowledgeCaptureService;
import com.secondbrain.service.RawChatRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka消费者服务
 * 监听聊天采集队列，将记录入库并触发知识提取
 */
@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final RawChatRecordService rawChatRecordService;
    private final KnowledgeCaptureService knowledgeCaptureService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(RawChatRecordService rawChatRecordService,
                                KnowledgeCaptureService knowledgeCaptureService,
                                ObjectMapper objectMapper) {
        this.rawChatRecordService = rawChatRecordService;
        this.knowledgeCaptureService = knowledgeCaptureService;
        this.objectMapper = objectMapper;
    }

    /**
     * 消费聊天采集消息
     *
     * @param record 原始聊天记录
     */
    @KafkaListener(topics = "chat-collect", groupId = "chat-collect-group")
    public void consumeChatCollect(RawChatRecord record) {
        log.info("收到聊天采集记录，userId：{}，sourceUrl：{}", record.getUserId(), record.getSourceUrl());
        
        try {
            rawChatRecordService.save(record);
            log.info("聊天记录保存成功，id：{}", record.getId());
            
            KnowledgeNode node = knowledgeCaptureService.extractKnowledge(record);
            if (node != null) {
                log.info("知识提取成功，nodeId：{}，title：{}", node.getId(), node.getTitle());
            }
        } catch (Exception e) {
            log.error("处理聊天采集记录失败", e);
        }
    }
}
