package com.secondbrain.dto;

import com.secondbrain.entity.RawChatRecord;
import lombok.Getter;
import lombok.Setter;

/**
 * Kafka chat-collect 消息体.
 * <p>封装 RawChatRecord + 采集元数据</p>
 */
@Getter
@Setter
public class ChatCollectMessage {

    /**
     * 原始对话记录
     */
    private RawChatRecord record;

    /**
     * 目标工作区ID
     */
    private Long workspaceId;

    /**
     * 是否提取知识点
     */
    private Boolean extractKnowledge;

    /**
     * 确认入库时是否生成复习卡片
     */
    private Boolean generateCards;
}
