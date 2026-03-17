package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识图谱边")
public class GraphEdge {

    @Schema(description = "源节点ID")
    private String source;

    @Schema(description = "目标节点ID")
    private String target;

    @Schema(description = "关系类型")
    private String label;

    @Schema(description = "关系强度")
    private Integer strength;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getStrength() {
        return strength;
    }

    public void setStrength(Integer strength) {
        this.strength = strength;
    }
}
