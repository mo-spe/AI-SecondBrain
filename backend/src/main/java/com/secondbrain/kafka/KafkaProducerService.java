package com.secondbrain.kafka;

import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Kafka生产者服务. <p>负责向Kafka发送聊天采集等消息</p> */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 构造器注入 KafkaTemplate.
     *
     * @param kafkaTemplate Kafka 模板
     */
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送聊天采集记录到Kafka.
     *
     * @param record 原始聊天记录
     * @return void
     */
    public void sendChatCollect(RawChatRecord record) {
        log.info("发送聊天采集记录到 Kafka，userId：{}，sourceUrl：{}", record.getUserId(), record.getSourceUrl());
        kafkaTemplate.send("chat-collect", record);
        log.info("聊天采集记录发送成功");
    }

    /**
     * 发送消息到指定主题.
     *
     * @param topic   主题名称
     * @param message 消息内容
     */
    public void sendMessage(String topic, Object message) {
        log.info("发送消息到 Kafka 主题：{}", topic);
        kafkaTemplate.send(topic, message);
        log.info("消息发送成功");
    }

    /**
     * 发送异步任务到Kafka.
     *
     * @param request 异步任务请求
     * @return void
     */
    public void sendAsyncTask(com.secondbrain.dto.AsyncTaskRequest request) {
        log.info("发送异步任务到 Kafka，taskId：{}，type：{}", request.getTaskId(), request.getTaskType());
        kafkaTemplate.send("async-tasks", request);
        log.info("异步任务发送成功");
    }
}
