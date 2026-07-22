package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 研究历史请求DTO.
 */
@Getter
@Setter
public class ResearchHistoryRequest {

    /**
     * 研究类型
     */
    private String type;

    /**
     * 研究主题
     */
    private String topic;

    /**
     * 研究内容
     */
    private String content;

    /**
     * 当前水平
     */
    private String currentLevel;

    /**
     * 目标水平
     */
    private String targetLevel;

    /**
     * 研究深度
     */
    private String depth;

    /**
     * 用户已有知识列表
     */
    private List<String> userKnowledge;

    /**
     * 涉及知识点数量
     */
    private Integer knowledgeCount;
}
