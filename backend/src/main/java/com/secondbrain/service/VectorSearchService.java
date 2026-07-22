package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeReference;

import java.util.List;

/**
 * 向量搜索服务接口.
 * <p>提供基于向量的语义搜索和相似度计算功能</p>
 */
public interface VectorSearchService {

    /**
     * 搜索相似知识点.
     *
     * @param question 查询问题
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 知识引用列表
     */
    List<KnowledgeReference> searchSimilar(String question, Long userId, int topK);

    /**
     * 搜索相似知识点（带API Key）.
     *
     * @param question 查询问题
     * @param userId 用户ID
     * @param topK 返回数量
     * @param userApiKey 用户API Key
     * @return 知识引用列表
     */
    List<KnowledgeReference> searchSimilar(String question, Long userId, int topK, String userApiKey);

    /**
     * 计算向量相似度.
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度（0-1）
     */
    double calculateSimilarity(List<Float> vec1, List<Float> vec2);
}
