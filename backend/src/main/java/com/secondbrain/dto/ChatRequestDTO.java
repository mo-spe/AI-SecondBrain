package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 聊天请求DTO.
 * <p>用于接收用户发送的聊天请求</p>
 */
@Getter
@Setter
public class ChatRequestDTO {

    /**
     * 会话ID（为空则创建新会话）
     */
    private Long sessionId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 模型名称（为空则使用默认模型）
     */
    private String model;
}
