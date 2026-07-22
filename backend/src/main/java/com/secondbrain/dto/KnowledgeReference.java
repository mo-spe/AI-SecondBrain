package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 知识引用信息DTO.
 */
@Getter
@Setter
public class KnowledgeReference {

    /**
     * 知识节点ID
     */
    private Long knowledgeId;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识摘要
     */
    private String summary;

    /**
     * 相关度分数（0-1）
     */
    private Double similarity;

    /**
     * 匹配的内容片段
     */
    private String matchedContent;
}
