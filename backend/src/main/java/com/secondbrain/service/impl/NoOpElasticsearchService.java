package com.secondbrain.service.impl;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.service.ElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/** Elasticsearch空操作实现类. <p>Elasticsearch未启用时的空操作实现</p> */
@Service
@ConditionalOnMissingBean(name = "elasticsearchServiceImpl")
public class NoOpElasticsearchService implements ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(NoOpElasticsearchService.class);

    /**
     * 同步知识节点到Elasticsearch（空操作）.
     *
     * @param node 知识节点
     * @return void
     */
    @Override
    public void syncKnowledgeNode(KnowledgeNode node) {
        log.debug("Elasticsearch未配置，跳过同步知识节点，nodeId：{}", node.getId());
    }

    /**
     * 删除知识节点（空操作）.
     *
     * @param id 知识节点ID
     * @return void
     */
    @Override
    public void deleteKnowledgeNode(Long id) {
        log.debug("Elasticsearch未配置，跳过删除知识节点，nodeId：{}", id);
    }

    /**
     * 关键词搜索（空操作）.
     *
     * @param keyword 关键词
     * @param userId  用户ID
     * @return 空结果列表
     */
    @Override
    public List<KnowledgeDocument> search(String keyword, Long userId) {
        log.debug("Elasticsearch未配置，返回空搜索结果，keyword：{}，userId：{}", keyword, userId);
        return Collections.emptyList();
    }

    /**
     * 多字段搜索（空操作）.
     *
     * @param keyword 关键词
     * @param userId  用户ID
     * @return 空结果列表
     */
    @Override
    public List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId) {
        log.debug("Elasticsearch未配置，返回空多字段搜索结果，keyword：{}，userId：{}", keyword, userId);
        return Collections.emptyList();
    }

    /**
     * 语义搜索（空操作）.
     *
     * @param queryText 查询文本
     * @param userId    用户ID
     * @param topK      返回数量上限
     * @return 空结果列表
     */
    @Override
    public List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK) {
        log.debug("Elasticsearch未配置，返回空语义搜索结果，queryText：{}，userId：{}，topK：{}", queryText, userId, topK);
        return Collections.emptyList();
    }
}