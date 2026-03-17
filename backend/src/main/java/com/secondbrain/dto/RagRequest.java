package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG问答请求")
public class RagRequest {
    
    @Schema(description = "用户问题", required = true, example = "什么是JVM垃圾回收？")
    private String question;
    
    @Schema(description = "检索的知识数量", defaultValue = "3")
    private Integer topK = 3;
    
    @Schema(description = "是否返回详细引用", defaultValue = "true")
    private Boolean includeReferences = true;
    
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public Boolean getIncludeReferences() {
        return includeReferences;
    }
    
    public void setIncludeReferences(Boolean includeReferences) {
        this.includeReferences = includeReferences;
    }
}
