package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** DeerFlow研究请求DTO. <p>用于深度学习研究请求参数封装</p> */
@Getter
@Setter
public class DeerFlowResearchRequest {

    /**
     * 学习数据
     */
    private String learningData;

    /**
     * 研究主题
     */
    private String topic;

    /**
     * 学习目标
     */
    private String goal;

    /**
     * 研究深度（beginner/intermediate/advanced）
     */
    private String depth;

    /**
     * 当前水平
     */
    private String currentLevel;

    /**
     * 目标水平
     */
    private String targetLevel;

    /**
     * 用户已有知识列表
     */
    private List<String> userKnowledge;
}
