package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 复习卡片视图对象.
 * <p>包含卡片详情及关联知识点的部分信息</p>
 */
@Getter
@Setter
public class ReviewCardVO {

    /**
     * 卡片ID
     */
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long nodeId;

    /**
     * 知识点标题
     */
    private String nodeTitle;

    /**
     * 知识点摘要
     */
    private String nodeSummary;

    /**
     * 题目内容
     */
    private String question;

    /**
     * 正确答案
     */
    private String answer;

    /**
     * 卡片类型
     */
    private String cardType;

    /**
     * 难度等级（1-5）
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
     * 记忆强度（0-1）
     */
    private Double memoryStrength;

    /**
     * 最后复习时间
     */
    private String lastReviewTime;

    /**
     * 下次复习时间
     */
    private String nextReviewTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否AI生成
     */
    private String aiGenerated;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 知识点复习次数
     */
    private Integer nodeReviewCount;

    /**
     * 知识点掌握程度
     */
    private Integer nodeMasteryLevel;

    /**
     * 是否已恢复
     */
    private Integer isRestored;
}
