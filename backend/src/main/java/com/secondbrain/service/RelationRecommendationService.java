package com.secondbrain.service;

import com.secondbrain.dto.RelationRecommendation;

import java.util.List;

/**
 * 关系推荐服务接口.
 * <p>基于知识点内容推荐潜在的知识关系</p>
 */
public interface RelationRecommendationService {

    /**
     * 推荐知识关系.
     *
     * @param knowledgeId 知识点ID
     * @param userId 用户ID
     * @return 关系推荐列表
     */
    List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId);

    /**
     * 推荐知识关系（指定数量）.
     *
     * @param knowledgeId 知识点ID
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 关系推荐列表
     */
    List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId, int topK);
}
