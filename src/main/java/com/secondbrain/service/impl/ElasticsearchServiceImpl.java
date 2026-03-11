package com.secondbrain.service.impl;

import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.elasticsearch.KnowledgeDocumentRepository;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.service.ElasticsearchService;
import com.secondbrain.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchServiceImpl.class);

    private static final double SEMANTIC_SEARCH_THRESHOLD = 0.3
    ;

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Override
    public void syncKnowledgeNode(KnowledgeNode node) {
        if (knowledgeDocumentRepository == null) {
            log.warn("Elasticsearch未配置，跳过同步，nodeId：{}", node.getId());
            return;
        }
        try {
            KnowledgeDocument document = new KnowledgeDocument();
            document.setId(node.getId());
            document.setUserId(node.getUserId());
            document.setTitle(node.getTitle());
            document.setSummary(node.getSummary());
            document.setContentMd(node.getContentMd());
            document.setImportance(node.getImportance());
            document.setMasteryLevel(node.getMasteryLevel());
            document.setReviewCount(node.getReviewCount());

            if (embeddingService != null) {
                String text = node.getTitle() + " " + node.getSummary();
                List<Float> embedding = embeddingService.generateEmbedding(text);
                if (!embedding.isEmpty()) {
                    document.setEmbedding(embedding);
                    log.info("Embedding生成成功，nodeId：{}，维度：{}", node.getId(), embedding.size());
                }
            }

            knowledgeDocumentRepository.save(document);
            log.info("知识点同步到Elasticsearch成功，nodeId：{}", node.getId());
        } catch (Exception e) {
            log.error("知识点同步到Elasticsearch失败，nodeId：{}", node.getId(), e);
        }
    }

    @Override
    public void deleteKnowledgeNode(Long id) {
        if (knowledgeDocumentRepository == null) {
            log.warn("Elasticsearch未配置，跳过删除，nodeId：{}", id);
            return;
        }
        try {
            knowledgeDocumentRepository.deleteById(id);
            log.info("知识点从Elasticsearch删除成功，nodeId：{}", id);
        } catch (Exception e) {
            log.error("知识点从Elasticsearch删除失败，nodeId：{}", id, e);
        }
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, Long userId) {
        if (knowledgeDocumentRepository == null) {
            log.warn("Elasticsearch未配置，无法搜索");
            return List.of();
        }
        try {
            List<KnowledgeDocument> results = knowledgeDocumentRepository.findByUserIdAndTitleContaining(userId, keyword);
            log.info("Elasticsearch搜索成功，keyword：{}，userId：{}，结果数：{}", keyword, userId, results.size());
            return results;
        } catch (Exception e) {
            log.error("Elasticsearch搜索失败，keyword：{}，userId：{}", keyword, userId, e);
            return List.of();
        }
    }

    @Override
    public List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId) {
        if (knowledgeDocumentRepository == null) {
            log.warn("Elasticsearch未配置，无法多字段搜索");
            return List.of();
        }
        try {
            List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findByUserId(userId);
            
            List<KnowledgeDocument> results = allDocs.stream()
                    .filter(doc -> {
                        boolean titleMatch = doc.getTitle() != null && doc.getTitle().contains(keyword);
                        boolean summaryMatch = doc.getSummary() != null && doc.getSummary().contains(keyword);
                        return titleMatch || summaryMatch;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Elasticsearch多字段搜索成功，keyword：{}，userId：{}，结果数：{}", keyword, userId, results.size());
            return results;
        } catch (Exception e) {
            log.error("Elasticsearch多字段搜索失败，keyword：{}，userId：{}", keyword, userId, e);
            return List.of();
        }
    }

    @Override
    public List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK) {
        if (embeddingService == null) {
            log.warn("Embedding服务未配置，无法进行语义搜索");
            return List.of();
        }
        try {
            List<Float> queryEmbedding = embeddingService.generateEmbedding(queryText);
            if (queryEmbedding.isEmpty()) {
                log.warn("查询向量生成失败，无法进行语义搜索");
                return List.of();
            }

            log.info("开始语义搜索，查询文本：'{}'，userId：{}", queryText, userId);

            List<KnowledgeDocument> allDocuments = knowledgeDocumentRepository.findByUserId(userId);
            log.info("从Elasticsearch获取到{}个文档", allDocuments.size());
            
            int docsWithEmbedding = 0;
            for (KnowledgeDocument doc : allDocuments) {
                if (doc.getEmbedding() != null && !doc.getEmbedding().isEmpty()) {
                    docsWithEmbedding++;
                } else {
                    log.debug("文档ID={}, 标题='{}' 没有embedding数据", doc.getId(), doc.getTitle());
                }
            }
            log.info("其中有{}个文档包含embedding数据", docsWithEmbedding);
            
            List<KnowledgeDocument> results = allDocuments.stream()
                    .filter(doc -> doc.getEmbedding() != null && !doc.getEmbedding().isEmpty())
                    .sorted((doc1, doc2) -> {
                        double similarity1 = calculateCosineSimilarity(queryEmbedding, doc1.getEmbedding());
                        double similarity2 = calculateCosineSimilarity(queryEmbedding, doc2.getEmbedding());
                        return Double.compare(similarity2, similarity1);
                    })
                    .filter(doc -> {
                        double similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
                        return similarity >= SEMANTIC_SEARCH_THRESHOLD;
                    })
                    .limit(topK)
                    .collect(java.util.stream.Collectors.toList());

            log.info("语义搜索成功，queryText：'{}'，userId：{}，结果数：{}，相似度阈值：{}", 
                queryText, userId, results.size(), SEMANTIC_SEARCH_THRESHOLD);
            
            if (!results.isEmpty()) {
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    KnowledgeDocument doc = results.get(i);
                    double similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
                    log.info("  结果{}: 标题='{}', 相似度={}", i+1, doc.getTitle(), String.format("%.4f", similarity));
                }
            } else {
                log.warn("没有找到相似度超过{}的结果", SEMANTIC_SEARCH_THRESHOLD);
            }
            
            return results;
        } catch (Exception e) {
            log.error("语义搜索失败，queryText：'{}'，userId：{}", queryText, userId, e);
            return List.of();
        }
    }

    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
