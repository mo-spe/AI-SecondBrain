package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 聊天响应DTO.
 * <p>用于返回AI聊天响应</p>
 */
@Getter
@Setter
public class ChatResponseDTO {

    /**
     * AI回复内容
     */
    private String content;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 使用的模型
     */
    private String model;
}
