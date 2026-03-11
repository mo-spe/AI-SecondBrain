package com.secondbrain.kafka;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.entity.RawChatRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String CHAT_COLLECT_TOPIC = "chat-collect-topic";

    public void sendChatCollect(RawChatRecord chatRecord) {
        try {
            String message = JSON.toJSONString(chatRecord);
            kafkaTemplate.send(CHAT_COLLECT_TOPIC, message);
            log.info("对话采集消息已发送到Kafka，topic：{}，消息：{}", CHAT_COLLECT_TOPIC, message);
        } catch (Exception e) {
            log.error("发送对话采集消息到Kafka失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
