package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识关系请求")
public class KnowledgeRelationRequest {

    @Schema(description = "源知识节点ID", required = true)
    private Long sourceId;

    @Schema(description = "目标知识节点ID", required = true)
    private Long targetId;

    @Schema(description = "关系类型（contains, depends, related, inherits, implements）", required = true)
    private String relationType;

    @Schema(description = "关系名称")
    private String relationName;

    @Schema(description = "关系强度（1-5）")
    private Integer relationStrength;

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationName() {
        return relationName;
    }

    public void setRelationName(String relationName) {
        this.relationName = relationName;
    }

    public Integer getRelationStrength() {
        return relationStrength;
    }

    public void setRelationStrength(Integer relationStrength) {
        this.relationStrength = relationStrength;
    }
}