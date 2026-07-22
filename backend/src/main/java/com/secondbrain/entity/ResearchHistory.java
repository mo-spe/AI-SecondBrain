package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究历史实体类.
 * <p>记录AI研究相关的操作历史，如学习报告生成、知识盲区分析等</p>
 */
@Getter
@Setter
@TableName("research_history")
public class ResearchHistory {

    /**
     * 历史记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 研究类型（LEARNING_REPORT/LEARNING_PATH/KNOWLEDGE_BLIND_SPOT）
     */
    private String type;

    /**
     * 研究主题
     */
    private String topic;

    /**
     * 研究内容/结果
     */
    private String content;

    /**
     * 当前水平
     */
    private String currentLevel;

    /**
     * 目标水平
     */
    private String targetLevel;

    /**
     * 研究深度（beginner/intermediate/advanced）
     */
    private String depth;

    /**
     * 用户已有知识（JSON格式）
     */
    private String userKnowledge;

    /**
     * 涉及知识点数量
     */
    private Integer knowledgeCount;

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
