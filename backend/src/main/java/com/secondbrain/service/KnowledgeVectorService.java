package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;

/**
 * 知识向量服务接口.
 * <p>提供知识向量的生成、保存、批量处理等功能</p>
 */
public interface KnowledgeVectorService {

    /**
     * 生成并保存向量.
     *
     * @param node 知识节点
     */
    void generateAndSaveVector(KnowledgeNode node);

    /**
     * 批量生成向量.
     *
     * @param userId 用户ID
     */
    void batchGenerateVectors(Long userId);

    /**
     * 重新生成向量.
     *
     * @param knowledgeId 知识点ID
     */
    void regenerateVector(Long knowledgeId);
}
