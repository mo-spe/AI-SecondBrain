package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import com.secondbrain.service.ElasticsearchService;
import com.secondbrain.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchServiceImpl.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private KnowledgeEmbeddingMapper embeddingMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public void syncKnowledgeNode(KnowledgeNode node) {
        try {
            log.debug("同步知识节点到Elasticsearch，nodeId：{}，title：{}", node.getId(), node.getTitle());

            KnowledgeDocument document = convertToDocument(node);

            elasticsearchOperations.save(document);

            log.info("知识节点同步成功，nodeId：{}", node.getId());

        } catch (Exception e) {
            log.error("同步知识节点到Elasticsearch失败，nodeId：{}", node.getId(), e);
        }
    }

    @Override
    public void deleteKnowledgeNode(Long id) {
        try {
            log.debug("从Elasticsearch删除知识节点，nodeId：{}", id);
            
            elasticsearchOperations.delete(String.valueOf(id), KnowledgeDocument.class);
            
            log.info("知识节点删除成功，nodeId：{}", id);
        } catch (Exception e) {
            log.error("从Elasticsearch删除知识节点失败，nodeId：{}", id, e);
        }
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, Long userId) {
        try {
            log.debug("关键词搜索，keyword：{}，userId：{}", keyword, userId);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .multiMatch(mm -> mm
                                                    .query(keyword)
                                                    .fields("title^3", "summary^2", "contentMd")
                                                    .fuzziness("AUTO")
                                            )
                                    )
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("userId")
                                                    .value(userId)
                                            )
                                    )
                            )
                    )
                    .build();

            SearchHits<KnowledgeDocument> searchHits = elasticsearchOperations.search(query, KnowledgeDocument.class);

            List<KnowledgeDocument> results = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("关键词搜索完成，keyword：{}，结果数：{}", keyword, results.size());
            return results;

        } catch (Exception e) {
            log.error("关键词搜索失败，keyword：{}", keyword, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<KnowledgeDocument> multiFieldSearch(String keyword, Long userId) {
        try {
            log.debug("多字段搜索，keyword：{}，userId：{}", keyword, userId);

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q
                            .bool(b -> b
                                    .should(s -> s
                                            .match(m -> m
                                                    .field("title")
                                                    .query(keyword)
                                                    .boost(3.0f)
                                            )
                                    )
                                    .should(s -> s
                                            .match(m -> m
                                                    .field("summary")
                                                    .query(keyword)
                                                    .boost(2.0f)
                                            )
                                    )
                                    .should(s -> s
                                            .match(m -> m
                                                    .field("contentMd")
                                                    .query(keyword)
                                            )
                                    )
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("userId")
                                                    .value(userId)
                                            )
                                    )
                            )
                    )
                    .build();

            SearchHits<KnowledgeDocument> searchHits = elasticsearchOperations.search(query, KnowledgeDocument.class);

            List<KnowledgeDocument> results = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("多字段搜索完成，keyword：{}，结果数：{}", keyword, results.size());
            return results;

        } catch (Exception e) {
            log.error("多字段搜索失败，keyword：{}", keyword, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<KnowledgeDocument> semanticSearch(String queryText, Long userId, int topK) {
        try {
            log.debug("语义搜索，queryText：{}，userId：{}，topK：{}", queryText, userId, topK);

            List<Float> queryEmbedding = embeddingService.generateEmbedding(queryText);
            
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                log.warn("无法生成查询向量，使用关键词搜索");
                return search(queryText, userId);
            }

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q
                            .bool(b -> b
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("userId")
                                                    .value(userId)
                                            )
                                    )
                            )
                    )
                    .withPageable(org.springframework.data.domain.PageRequest.of(0, 100))
                    .build();

            SearchHits<KnowledgeDocument> searchHits = elasticsearchOperations.search(query, KnowledgeDocument.class);

            List<KnowledgeDocument> allDocuments = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .filter(doc -> doc.getEmbedding() != null && !doc.getEmbedding().isEmpty())
                    .collect(Collectors.toList());

            List<KnowledgeDocument> results = allDocuments.stream()
                    .map(doc -> {
                        double similarity = calculateCosineSimilarity(queryEmbedding, doc.getEmbedding());
                        doc.setSimilarity(similarity);
                        return doc;
                    })
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("语义搜索完成，queryText：{}，结果数：{}", queryText, results.size());
            return results;

        } catch (Exception e) {
            log.error("语义搜索失败，queryText：{}", queryText, e);
            return new ArrayList<>();
        }
    }

    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private KnowledgeDocument convertToDocument(KnowledgeNode node) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(node.getId());
        document.setUserId(node.getUserId());
        document.setTitle(node.getTitle());
        document.setSummary(node.getSummary());
        document.setContentMd(node.getContentMd());
        document.setImportance(node.getImportance());
        document.setMasteryLevel(node.getMasteryLevel());
        document.setReviewCount(node.getReviewCount());
        
        try {
            KnowledgeEmbedding embedding = embeddingMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeEmbedding>()
                            .eq(KnowledgeEmbedding::getKnowledgeId, node.getId())
            );
            
            if (embedding != null && embedding.getEmbedding() != null) {
                List<Float> vector = JSON.parseArray(embedding.getEmbedding(), Float.class);
                document.setEmbedding(vector);
            }
        } catch (Exception e) {
            log.warn("获取向量数据失败，nodeId：{}", node.getId(), e);
        }
        
        return document;
    }
}
