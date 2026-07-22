package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 知识图谱DTO.
 */
@Getter
@Setter
public class KnowledgeGraph {

    /**
     * 节点列表
     */
    private List<GraphNode> nodes;

    /**
     * 边列表
     */
    private List<GraphEdge> edges;
}
