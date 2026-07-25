package com.secondbrain.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Kafka生产者服务. <p>负责向Kafka发送聊天采集等消息</p> */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造器注入 KafkaTemplate 和 ObjectMapper.
     *
     * @param kafkaTemplate Kafka 模板
     * @param objectMapper  JSON 序列化工具
     */
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送聊天采集消息到Kafka（新接口，携带元数据）.
     *
     * @param message 聊天采集消息（包含 RawChatRecord + 工作区/提取/卡片控制字段）
     */
    public void sendChatCollect(ChatCollectMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            log.info("发送聊天采集消息到 Kafka，userId={} workspaceId={} extractKnowledge={} generateCards={}",
                    message.getRecord().getUserId(), message.getWorkspaceId(),
                    message.getExtractKnowledge(), message.getGenerateCards());
            kafkaTemplate.send("chat-collect", json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka 发送失败 recordId={}", message.getRecord().getId(), ex);
                        } else {
                            log.info("Kafka 发送成功 recordId={} offset={}",
                                    message.getRecord().getId(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("聊天采集消息序列化失败", e);
        }
    }

    /**
     * 发送聊天采集记录到Kafka（旧接口，向后兼容）.
     *
     * @deprecated 使用 {@link #sendChatCollect(ChatCollectMessage)} 替代
     * @param record 原始聊天记录
     */
    @Deprecated
    public void sendChatCollect(RawChatRecord record) {
        ChatCollectMessage message = new ChatCollectMessage();
        message.setRecord(record);
        message.setWorkspaceId(record.getWorkspaceId());
        message.setExtractKnowledge(true);
        message.setGenerateCards(false);
        sendChatCollect(message);
    }

    /**
     * 发送消息到指定主题.
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    public void sendMessage(String topic, Object message) {
        try {
            String json = message instanceof String ? (String) message : objectMapper.writeValueAsString(message);
            log.info("发送消息到 Kafka 主题：{}", topic);
            kafkaTemplate.send(topic, json);
            log.info("消息发送成功");
        } catch (Exception e) {
            log.error("发送消息到 Kafka 失败，topic={}", topic, e);
        }
    }

    /**
     * 发送异步任务到Kafka.
     *
     * @param request 异步任务请求
     */
    public void sendAsyncTask(com.secondbrain.dto.AsyncTaskRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.info("发送异步任务到 Kafka，taskId：{}，type：{}", request.getTaskId(), request.getTaskType());
            kafkaTemplate.send("async-tasks", json);
            log.info("异步任务发送成功");
        } catch (Exception e) {
            log.error("发送异步任务失败，taskId={}", request.getTaskId(), e);
        }
    }
}
