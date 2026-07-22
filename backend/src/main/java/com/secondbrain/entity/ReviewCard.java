package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复习卡片实体类.
 * <p>存储基于知识点生成的复习题目，支持选择题、填空题、简答题等类型</p>
 */
@Getter
@Setter
@TableName("review_card")
public class ReviewCard {

    /**
     * 卡片ID
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
     * 题目内容
     */
    private String question;

    /**
     * 正确答案
     */
    private String answer;

    /**
     * 卡片类型（choice-选择题，fill-填空题，essay-简答题，judge-判断题）
     */
    private String cardType;

    /**
     * 难度等级（1-5，5最高）
     */
    private Integer difficulty;

    /**
     * 复习次数
     */
    private Integer reviewCount;

    /**
     * 正确次数
     */
    private Integer correctCount;

    /**
     * 错误次数
     */
    private Integer incorrectCount;

    /**
     * 掌握程度（0-5）
     */
    private Integer masteryLevel;

    /**
     * 记忆强度（0-1，1最强）
     */
    private Double memoryStrength;

    /**
     * 最后复习时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 状态（0-待复习，1-已掌握，2-已暂停）
     */
    private Integer status;

    /**
     * 是否AI生成（true/false）
     */
    private String aiGenerated;

    /**
     * 生成类型
     */
    private String generationType;

    /**
     * 是否已恢复（0-未恢复，1-已恢复）
     */
    private Integer isRestored;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标记（逻辑删除）
     */
    @TableLogic
    private Integer deleted;
}
