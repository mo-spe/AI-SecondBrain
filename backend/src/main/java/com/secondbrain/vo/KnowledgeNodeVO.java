package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 知识节点视图对象. <p>用于向前端返回知识节点详情数据</p> */
@Getter
@Setter
public class KnowledgeNodeVO {

    /**
     * 节点ID
     */
    private Long id;

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

    /**
     * 掌握程度（0-5）
     */
    private Integer masteryLevel;

    /**
     * 复习次数
     */
    private Integer reviewCount;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 搜索分数（语义搜索相关度）
     */
    private Double score;
}
