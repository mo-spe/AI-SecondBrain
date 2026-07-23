package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识关系实体类.
 * <p>存储知识点之间的关联关系，如父子关系、依赖关系等</p>
 */
@Getter
@Setter
@TableName("knowledge_relation")
public class KnowledgeRelation {

    /**
     * 关系ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作区ID
     */
    private Long workspaceId;

    /**
     * 源知识点ID
     */
    private Long fromKnowledgeId;

    /**
     * 目标知识点ID
     */
    private Long toKnowledgeId;

    /**
     * 关系类型（parent-父子，depends-依赖，similar-相似，related-关联）
     */
    private String relationType;

    /**
     * 关系名称（显示用）
     */
    private String relationName;

    /**
     * 关系权重（0-1，用于相似度计算）
     */
    private Double weight;

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
