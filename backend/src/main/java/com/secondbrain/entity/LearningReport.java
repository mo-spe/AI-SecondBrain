package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 学习报告实体类.
 * <p>存储AI生成的学习报告内容</p>
 */
@Getter
@Setter
@TableName("learning_report")
public class LearningReport {

    /**
     * 报告ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 报告主题
     */
    private String topic;

    /**
     * 报告内容
     */
    private String content;

    /**
     * 学习天数
     */
    private Integer days;

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
