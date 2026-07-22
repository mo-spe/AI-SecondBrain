package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeGraph;
import com.secondbrain.dto.KnowledgeRelationRequest;

/**
 * 知识图谱服务接口.
 * <p>提供知识图谱的查询、关系管理等功能</p>
 */
public interface KnowledgeGraphService {

    /**
     * 获取知识图谱.
     *
     * @param userId 用户ID
     * @return 知识图谱
     */
    KnowledgeGraph getGraph(Long userId);

    /**
     * 添加知识关系.
     *
     * @param request 关系请求
     * @param userId 用户ID
     */
    void addRelation(KnowledgeRelationRequest request, Long userId);

    /**
     * 删除知识关系.
     *
     * @param relationId 关系ID
     * @param userId 用户ID
     */
    void deleteRelation(Long relationId, Long userId);

    /**
     * 自动生成知识关系.
     *
     * @param userId 用户ID
     */
    void autoGenerateRelations(Long userId);
}
