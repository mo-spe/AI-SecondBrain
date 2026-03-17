package com.secondbrain.service;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeNode;

import java.util.List;

public interface ElasticsearchService {

    void syncKnowledgeNode(KnowledgeNode node);

    void deleteKnowledgeNode(Long id);

    List<KnowledgeDocument> search(String keyword, Long userId);

    List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId);

    List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK);
}
