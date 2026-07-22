package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 知识图谱边DTO.
 */
@Getter
@Setter
public class GraphEdge {

    /**
     * 源节点ID
     */
    private String source;

    /**
     * 目标节点ID
     */
    private String target;

    /**
     * 关系类型（显示标签）
     */
    private String label;

    /**
     * 关系强度（1-5）
     */
    private Integer strength;
}
