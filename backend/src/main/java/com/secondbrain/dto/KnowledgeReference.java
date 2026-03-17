package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识引用信息")
public class KnowledgeReference {
    
    @Schema(description = "知识节点ID")
    private Long knowledgeId;
    
    @Schema(description = "知识标题")
    private String title;
    
    @Schema(description = "知识摘要")
    private String summary;
    
    @Schema(description = "相关度分数")
    private Double similarity;
    
    @Schema(description = "匹配的内容片段")
    private String matchedContent;
    
    public Long getKnowledgeId() {
        return knowledgeId;
    }
    
    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
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
    
    public Double getSimilarity() {
        return similarity;
    }
    
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
    
    public String getMatchedContent() {
        return matchedContent;
    }
    
    public void setMatchedContent(String matchedContent) {
        this.matchedContent = matchedContent;
    }
}
