package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天消息DTO.
 * <p>用于传输聊天消息信息</p>
 */
@Getter
@Setter
public class ChatMessageDTO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 关联会话ID
     */
    private Long sessionId;

    /**
     * 消息角色
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
