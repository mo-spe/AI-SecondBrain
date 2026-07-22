package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 关系推荐DTO.
 */
@Getter
@Setter
public class RelationRecommendation {

    /**
     * 目标知识节点信息
     */
    private KnowledgeNodeInfo targetKnowledge;

    /**
     * 相似度（0-1）
     */
    private Double similarity;

    /**
     * 推荐的关系类型
     */
    private String recommendedType;

    /**
     * 推荐的关系名称
     */
    private String recommendedTypeName;

    /**
     * 知识节点信息内部类.
     */
    @Getter
    @Setter
    public static class KnowledgeNodeInfo {

        /**
         * 知识节点ID
         */
        private Long id;

        /**
         * 知识节点标题
         */
        private String title;

        /**
         * 知识节点摘要
         */
        private String summary;

        /**
         * 重要程度（1-5）
         */
        private Integer importance;

        /**
         * 掌握程度（0-5）
         */
        private Integer masteryLevel;
    }
}
