package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * RAG问答响应DTO.
 */
@Getter
@Setter
public class RagResponse {

    /**
     * AI生成的答案
     */
    private String answer;

    /**
     * 引用的知识列表
     */
    private List<KnowledgeReference> references;

    /**
     * 检索耗时（毫秒）
     */
    private Long retrievalTime;

    /**
     * 生成耗时（毫秒）
     */
    private Long generationTime;
}
