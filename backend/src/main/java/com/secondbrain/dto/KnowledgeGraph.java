package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "知识图谱")
public class KnowledgeGraph {

    @Schema(description = "节点列表")
    private List<GraphNode> nodes;

    @Schema(description = "边列表")
    private List<GraphEdge> edges;

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphEdge> edges) {
        this.edges = edges;
    }
}
