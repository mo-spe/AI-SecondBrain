package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 知识点DTO. <p>用于知识点数据传输</p> */
@Getter
@Setter
public class KnowledgeDTO {

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 内容
     */
    private String content;

    /**
     * 关键词列表
     */
    private List<String> keywords;
}
