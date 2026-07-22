package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识向量实体类.
 * <p>存储知识点的Embedding向量数据，用于语义搜索</p>
 */
@Getter
@Setter
@TableName("knowledge_embedding")
public class KnowledgeEmbedding {

    /**
     * 向量ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long knowledgeId;

    /**
     * 原始内容
     */
    private String content;

    /**
     * 向量数据（JSON数组格式）
     */
    private String embedding;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
