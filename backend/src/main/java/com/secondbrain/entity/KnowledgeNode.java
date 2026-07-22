package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识节点实体类.
 * <p>存储知识点的标题、内容、掌握程度等核心信息</p>
 */
@Getter
@Setter
@TableName("knowledge_node")
public class KnowledgeNode {

    /**
     * 知识点ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 知识点标题
     */
    private String title;

    /**
     * 内容（Markdown格式）
     */
    private String contentMd;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 向量ID
     */
    private String vectorId;

    /**
     * 重要程度（1-5，5最高）
     */
    private Integer importance;

    /**
     * 掌握程度（0-5，0未掌握，5已掌握）
     */
    private Integer masteryLevel;

    /**
     * 复习次数
     */
    private Integer reviewCount;

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
     * 最后复习时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 下次复习时间（基于艾宾浩斯曲线计算）
     */
    private LocalDateTime nextReviewTime;

    /**
     * 删除标记（逻辑删除）
     */
    @TableLogic
    private Integer deleted;
}
