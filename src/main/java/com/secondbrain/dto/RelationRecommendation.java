package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "关系推荐")
public class RelationRecommendation {

    @Schema(description = "目标知识节点")
    private KnowledgeNodeInfo targetKnowledge;

    @Schema(description = "相似度")
    private Double similarity;

    @Schema(description = "推荐的关系类型")
    private String recommendedType;

    @Schema(description = "推荐的关系名称")
    private String recommendedTypeName;

    public KnowledgeNodeInfo getTargetKnowledge() {
        return targetKnowledge;
    }

    public void setTargetKnowledge(KnowledgeNodeInfo targetKnowledge) {
        this.targetKnowledge = targetKnowledge;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public String getRecommendedType() {
        return recommendedType;
    }

    public void setRecommendedType(String recommendedType) {
        this.recommendedType = recommendedType;
    }

    public String getRecommendedTypeName() {
        return recommendedTypeName;
    }

    public void setRecommendedTypeName(String recommendedTypeName) {
        this.recommendedTypeName = recommendedTypeName;
    }

    @Schema(description = "知识节点信息")
    public static class KnowledgeNodeInfo {
        @Schema(description = "知识节点ID")
        private Long id;

        @Schema(description = "知识节点标题")
        private String title;

        @Schema(description = "知识节点摘要")
        private String summary;

        @Schema(description = "重要程度")
        private Integer importance;

        @Schema(description = "掌握程度")
        private Integer masteryLevel;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
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
    }
}