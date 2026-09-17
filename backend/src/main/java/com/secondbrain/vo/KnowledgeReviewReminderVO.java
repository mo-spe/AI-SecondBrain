package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识点指定复习提醒视图对象。
 */
@Getter
@Setter
public class KnowledgeReviewReminderVO {

    /**
     * 提醒ID。
     */
    private Long id;

    /**
     * 知识点ID。
     */
    private Long nodeId;

    /**
     * 知识点标题。
     */
    private String nodeTitle;

    /**
     * 计划提醒时间。
     */
    private LocalDateTime scheduledAt;

    /**
     * 当前提醒状态。
     */
    private String status;

    /**
     * 实际发送时间；未发送时为空。
     */
    private LocalDateTime sentAt;
}
