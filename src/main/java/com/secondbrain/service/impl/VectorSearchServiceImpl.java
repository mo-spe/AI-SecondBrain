package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.KnowledgeReference;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.EmbeddingService;
import com.secondbrain.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchServiceImpl.class);

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private KnowledgeEmbeddingMapper embeddingMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    private static final String defaultModel = "text-embedding-v2";

    @Override
    public List<KnowledgeReference> searchSimilar(String question, Long userId, int topK) {
        return searchSimilar(question, userId, topK, null);
    }

    @Override
    public List<KnowledgeReference> searchSimilar(String question, Long userId, int topK, String userApiKey) {
        log.info("开始向量检索，问题：'{}'，userId：{}，topK：{}", question, userId, topK);

        List<Float> questionEmbedding = embeddingService.generateEmbedding(question, defaultModel, userApiKey);
        
        if (questionEmbedding == null || questionEmbedding.isEmpty()) {
            log.error("问题向量化失败，无法进行检索");
            throw new RuntimeException("问题向量化失败，请检查API Key配置或稍后重试");
        }

        List<KnowledgeEmbedding> embeddings = embeddingMapper.getByUserId(userId);
        
        if (embeddings.isEmpty()) {
            log.warn("用户{}没有知识向量数据", userId);
            return new ArrayList<>();
        }

        Map<Long, KnowledgeNode> nodeMap = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .eq(KnowledgeNode::getDeleted, 0)
        ).stream().collect(Collectors.toMap(KnowledgeNode::getId, node -> node));

        List<SimilarityResult> results = new ArrayList<>();
        
        log.info("开始计算相似度，共{}个向量", embeddings.size());
        
        for (KnowledgeEmbedding embedding : embeddings) {
            List<Float> docEmbedding = parseEmbedding(embedding.getEmbedding());
            
            if (docEmbedding != null && !docEmbedding.isEmpty()) {
                double similarity = calculateSimilarity(questionEmbedding, docEmbedding);
                
                KnowledgeNode node = nodeMap.get(embedding.getKnowledgeId());
                if (node != null) {
                    SimilarityResult result = new SimilarityResult();
                    result.setNode(node);
                    result.setSimilarity(similarity);
                    results.add(result);
                    
                    log.debug("知识节点：{}，相似度：{}", node.getTitle(), similarity);
                }
            }
        }

        List<SimilarityResult> sortedResults = results.stream()
            .sorted(Comparator.comparingDouble(SimilarityResult::getSimilarity).reversed())
            .collect(Collectors.toList());
        
        log.info("相似度排序后的前{}个结果：", Math.min(topK, sortedResults.size()));
        for (int i = 0; i < Math.min(topK, sortedResults.size()); i++) {
            SimilarityResult result = sortedResults.get(i);
            log.info("  第{}名：{}，相似度：{}", i + 1, result.getNode().getTitle(), result.getSimilarity());
        }

        List<KnowledgeReference> references = sortedResults.stream()
            .limit(topK)
            .map(result -> {
                KnowledgeReference ref = new KnowledgeReference();
                ref.setKnowledgeId(result.getNode().getId());
                ref.setTitle(result.getNode().getTitle());
                ref.setSummary(result.getNode().getSummary());
                ref.setSimilarity(result.getSimilarity());
                return ref;
            })
            .collect(Collectors.toList());

        log.info("向量检索完成，返回{}个结果", references.size());
        
        return references;
    }

    @Override
    public double calculateSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }

        int minSize = Math.min(vec1.size(), vec2.size());
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < minSize; i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += Math.pow(vec1.get(i), 2);
            norm2 += Math.pow(vec2.get(i), 2);
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private List<Float> parseEmbedding(String embeddingJson) {
        try {
            return JSON.parseArray(embeddingJson, Float.class);
        } catch (Exception e) {
            log.error("解析向量失败：{}", embeddingJson, e);
            return null;
        }
    }

    private static class SimilarityResult {
        private KnowledgeNode node;
        private double similarity;

        public KnowledgeNode getNode() {
            return node;
        }

        public void setNode(KnowledgeNode node) {
            this.node = node;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }
    }
}