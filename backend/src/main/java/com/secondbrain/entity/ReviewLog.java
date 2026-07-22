package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复习日志实体类.
 * <p>记录每次复习的结果和时长</p>
 */
@Getter
@Setter
@TableName("review_log")
public class ReviewLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long nodeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 复习结果（correct-正确，incorrect-错误）
     */
    private String result;

    /**
     * 答题时长（秒）
     */
    private Integer duration;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标记（逻辑删除）
     */
    @TableLogic
    private Integer deleted;
}
