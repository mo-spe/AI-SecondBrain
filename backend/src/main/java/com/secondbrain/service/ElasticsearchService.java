package com.secondbrain.service;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeNode;

import java.util.List;

/**
 * Elasticsearch服务接口.
 * <p>提供知识点的同步、搜索等功能</p>
 */
public interface ElasticsearchService {

    /**
     * 同步知识点到Elasticsearch.
     *
     * @param node 知识节点
     */
    void syncKnowledgeNode(KnowledgeNode node);

    /**
     * 从Elasticsearch删除知识点.
     *
     * @param id 知识点ID
     */
    void deleteKnowledgeNode(Long id);

    /**
     * 关键词搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 知识文档列表
     */
    List<KnowledgeDocument> search(String keyword, Long userId);

    /**
     * 多字段搜索.
     *
     * @param keyword 关键词
     * @param userId 用户ID
     * @return 知识文档列表
     */
    List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId);

    /**
     * 语义搜索.
     *
     * @param queryText 查询文本
     * @param userId 用户ID
     * @param topK 返回数量
     * @return 知识文档列表
     */
    List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK);
}
