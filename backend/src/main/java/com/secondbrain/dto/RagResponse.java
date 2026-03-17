package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "RAG问答响应")
public class RagResponse {
    
    @Schema(description = "AI生成的答案")
    private String answer;
    
    @Schema(description = "引用的知识列表")
    private List<KnowledgeReference> references;
    
    @Schema(description = "检索耗时（毫秒）")
    private Long retrievalTime;
    
    @Schema(description = "生成耗时（毫秒）")
    private Long generationTime;
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public List<KnowledgeReference> getReferences() {
        return references;
    }
    
    public void setReferences(List<KnowledgeReference> references) {
        this.references = references;
    }
    
    public Long getRetrievalTime() {
        return retrievalTime;
    }
    
    public void setRetrievalTime(Long retrievalTime) {
        this.retrievalTime = retrievalTime;
    }
    
    public Long getGenerationTime() {
        return generationTime;
    }
    
    public void setGenerationTime(Long generationTime) {
        this.generationTime = generationTime;
    }
}
