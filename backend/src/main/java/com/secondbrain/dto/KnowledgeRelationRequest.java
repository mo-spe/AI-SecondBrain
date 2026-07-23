package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 知识关系请求DTO. <p>用于创建知识节点关系的请求参数</p> */
@Getter
@Setter
public class KnowledgeRelationRequest {

    /**
     * 源知识节点ID
     */
    private Long sourceId;

    /**
     * 目标知识节点ID
     */
    private Long targetId;

    /**
     * 关系类型（contains, depends, related, inherits, implements）
     */
    private String relationType;

    /**
     * 关系名称
     */
    private String relationName;

    /**
     * 关系强度（1-5）
     */
    private Integer relationStrength;
}
