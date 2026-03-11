package com.secondbrain.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends ElasticsearchRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByUserIdAndTitleContaining(Long userId, String title);

    List<KnowledgeDocument> findByUserIdAndSummaryContaining(Long userId, String summary);

    List<KnowledgeDocument> findByUserId(Long userId);
}
