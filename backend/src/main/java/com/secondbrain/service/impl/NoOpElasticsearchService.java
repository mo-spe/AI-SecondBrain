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

@Service
@ConditionalOnMissingBean(name = "elasticsearchServiceImpl")
public class NoOpElasticsearchService implements ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(NoOpElasticsearchService.class);

    @Override
    public void syncKnowledgeNode(KnowledgeNode node) {
        log.debug("Elasticsearch未配置，跳过同步知识节点，nodeId：{}", node.getId());
    }

    @Override
    public void deleteKnowledgeNode(Long id) {
        log.debug("Elasticsearch未配置，跳过删除知识节点，nodeId：{}", id);
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, Long userId) {
        log.debug("Elasticsearch未配置，返回空搜索结果，keyword：{}，userId：{}", keyword, userId);
        return Collections.emptyList();
    }

    @Override
    public List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId) {
        log.debug("Elasticsearch未配置，返回空多字段搜索结果，keyword：{}，userId：{}", keyword, userId);
        return Collections.emptyList();
    }

    @Override
    public List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK) {
        log.debug("Elasticsearch未配置，返回空语义搜索结果，queryText：{}，userId：{}，topK：{}", queryText, userId, topK);
        return Collections.emptyList();
    }
}