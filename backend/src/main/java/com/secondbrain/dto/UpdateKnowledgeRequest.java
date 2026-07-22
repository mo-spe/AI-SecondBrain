package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 更新知识点请求DTO.
 */
@Getter
@Setter
public class UpdateKnowledgeRequest {

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 内容（Markdown格式）
     */
    private String contentMd;

    /**
     * 重要程度（1-5）
     */
    private Integer importance;
}
