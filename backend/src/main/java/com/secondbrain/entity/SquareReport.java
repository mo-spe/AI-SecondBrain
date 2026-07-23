package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 举报记录实体.
 *
 * <p>用户可举报广场帖子，由 super_admin 处理（忽略/移除）。</p>
 */
@Getter
@Setter
@TableName("square_report")
public class SquareReport {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被举报帖子ID
     */
    private Long postId;

    /**
     * 举报人用户ID
     */
    private Long reporterId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 处理状态：pending / ignored / removed
     */
    private String status;

    /**
     * 处理人ID（admin）
     */
    private Long handlerId;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 举报时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
}
