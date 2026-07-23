package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知实体.
 *
 * <p>用于广场互动通知（点赞、评论、举报结果），与邮件通知服务分离。</p>
 */
@Getter
@Setter
@TableName("notification")
public class Notification {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID
     */
    private Long userId;

    /**
     * 通知类型：like / comment / report_result
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 关联目标类型：post
     */
    private String targetType;

    /**
     * 关联目标ID
     */
    private Long targetId;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 通知时间
     */
    private LocalDateTime createdAt;
}
