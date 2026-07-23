package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 对话记录DTO. <p>用于对话记录数据传输</p> */
@Getter
@Setter
public class ChatRecordDTO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 来源平台（wechat/chatgpt/other）
     */
    private String platform;

    /**
     * 对话内容
     */
    private String content;

    /**
     * 来源URL
     */
    private String sourceUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
