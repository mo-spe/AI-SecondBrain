package com.secondbrain.kafka;

import com.secondbrain.dto.AsyncTaskRequest;
import com.secondbrain.dto.ChatCollectMessage;
import com.secondbrain.entity.RawChatRecord;

/**
 * Abstraction for sending messages to Kafka.
 * Business code should depend on this interface so the active implementation
 * is chosen automatically based on {@code spring.kafka.enabled}.
 */
public interface KafkaMessageProducer {

    /**
     * Sends a chat-collect message.
     *
     * @param message chat-collect payload (RawChatRecord + workspace and extract/generate flags)
     */
    void sendChatCollect(ChatCollectMessage message);

    /**
     * Sends a raw chat record (legacy API).
     *
     * @param record raw chat record
     * @deprecated use {@link #sendChatCollect(ChatCollectMessage)} instead
     */
    @Deprecated
    void sendChatCollect(RawChatRecord record);

    /**
     * Sends a message to the given topic.
     *
     * @param topic   topic name
     * @param message message payload
     */
    void sendMessage(String topic, Object message);

    /**
     * Sends an async task request.
     *
     * @param request async task request
     */
    void sendAsyncTask(AsyncTaskRequest request);
}