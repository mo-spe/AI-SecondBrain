package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * RAG问答请求DTO.
 */
@Getter
@Setter
public class RagRequest {

    /**
     * 用户问题
     */
    private String question;

    /**
     * 检索的知识数量（默认3）
     */
    private Integer topK = 3;

    /**
     * 是否返回详细引用（默认true）
     */
    private Boolean includeReferences = true;
}
