package com.secondbrain.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 设置知识点指定复习提醒请求。
 */
@Getter
@Setter
public class SetKnowledgeReviewReminderRequest {

    /**
     * 提醒应触发的未来时间。
     */
    @NotNull(message = "提醒时间不能为空")
    @Future(message = "提醒时间必须晚于当前时间")
    private LocalDateTime scheduledAt;
}
