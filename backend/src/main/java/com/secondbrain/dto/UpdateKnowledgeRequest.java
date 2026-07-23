package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 更新知识请求DTO. <p>用于更新知识节点信息的请求参数</p> */
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
