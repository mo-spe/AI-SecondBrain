package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识点指定复习提醒实体。
 *
 * <p>一名用户对同一知识点只保留一条记录，重新预约会覆盖尚未生效或已经发送的旧计划。</p>
 */
@Getter
@Setter
@TableName("knowledge_review_reminder")
public class KnowledgeReviewReminder {

    /**
     * 提醒ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收提醒的用户ID。
     */
    private Long userId;

    /**
     * 关联知识点ID。
     */
    private Long nodeId;

    /**
     * 用户指定的提醒时间。
     */
    private LocalDateTime scheduledAt;

    /**
     * 发送状态：SCHEDULED 或 SENT。
     */
    private String status;

    /**
     * 站内通知成功写入后的发送时间。
     */
    private LocalDateTime sentAt;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
