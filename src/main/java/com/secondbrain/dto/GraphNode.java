package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识图谱节点")
public class GraphNode {

    @Schema(description = "节点ID")
    private String id;

    @Schema(description = "节点标签")
    private String label;

    @Schema(description = "节点类型")
    private String type;

    @Schema(description = "重要程度")
    private Integer importance;

    @Schema(description = "掌握程度")
    private Integer masteryLevel;

    @Schema(description = "节点大小")
    private Integer size;

    @Schema(description = "节点颜色")
    private String color;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
