package com.secondbrain.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

/**
 * 知识文档（Elasticsearch索引实体）.
 * <p>用于在Elasticsearch中存储和搜索知识点数据</p>
 */
@Getter
@Setter
@Document(indexName = "knowledge_nodes", createIndex = false)
public class KnowledgeDocument {

    /**
     * 文档ID（对应知识点ID）
     */
    @Id
    private Long id;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Keyword)
    private Long userId;

    /**
     * 知识点标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 摘要
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String summary;

    /**
     * 内容（Markdown格式）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String contentMd;

    /**
     * Embedding向量（1536维）
     */
    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private List<Float> embedding;

    /**
     * 重要程度
     */
    @Field(type = FieldType.Integer)
    private Integer importance;

    /**
     * 掌握程度
     */
    @Field(type = FieldType.Integer)
    private Integer masteryLevel;

    /**
     * 复习次数
     */
    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    /**
     * 相似度（搜索结果时使用）
     */
    private Double similarity;
}
