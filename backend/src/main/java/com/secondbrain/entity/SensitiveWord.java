package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 敏感词实体.
 *
 * <p>敏感词库由 super_admin 维护，用于广场发布和评论的自动过滤。</p>
 */
@Getter
@Setter
@TableName("sensitive_word")
public class SensitiveWord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 敏感词（唯一约束）
     */
    private String word;

    /**
     * 添加时间
     */
    private LocalDateTime createdAt;
}
