package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.RelationRecommendation;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.EmbeddingService;
import com.secondbrain.service.RelationRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelationRecommendationServiceImpl implements RelationRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RelationRecommendationServiceImpl.class);

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private KnowledgeEmbeddingMapper embeddingMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId) {
        return recommendRelations(knowledgeId, userId, 10);
    }

    @Override
    public List<RelationRecommendation> recommendRelations(Long knowledgeId, Long userId, int topK) {
        log.info("开始为知识节点{}推荐关系，userId：{}，topK：{}", knowledgeId, userId, topK);

        long startTime = System.currentTimeMillis();

        KnowledgeNode currentKnowledge = knowledgeNodeMapper.selectById(knowledgeId);
        if (currentKnowledge == null) {
            log.warn("知识节点{}不存在", knowledgeId);
            return new ArrayList<>();
        }

        if (!currentKnowledge.getUserId().equals(userId)) {
            log.warn("用户{}无权访问知识节点{}", userId, knowledgeId);
            return new ArrayList<>();
        }

        List<KnowledgeNode> allKnowledge = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .eq(KnowledgeNode::getDeleted, 0)
                .ne(KnowledgeNode::getId, knowledgeId)
        );

        if (allKnowledge.isEmpty()) {
            log.info("用户{}没有其他知识节点", userId);
            return new ArrayList<>();
        }

        List<KnowledgeEmbedding> currentEmbeddings = embeddingMapper.selectList(
            new LambdaQueryWrapper<KnowledgeEmbedding>()
                .eq(KnowledgeEmbedding::getKnowledgeId, knowledgeId)
                .orderByDesc(KnowledgeEmbedding::getCreateTime)
                .last("LIMIT 1")
        );

        if (currentEmbeddings.isEmpty()) {
            log.warn("知识节点{}没有向量数据", knowledgeId);
            return new ArrayList<>();
        }

        KnowledgeEmbedding currentEmbedding = currentEmbeddings.get(0);

        List<Float> currentVector = parseEmbedding(currentEmbedding.getEmbedding());
        if (currentVector == null || currentVector.isEmpty()) {
            log.warn("知识节点{}的向量解析失败", knowledgeId);
            return new ArrayList<>();
        }

        List<RelationRecommendation> recommendations = new ArrayList<>();

        for (KnowledgeNode otherKnowledge : allKnowledge) {
            List<KnowledgeEmbedding> otherEmbeddings = embeddingMapper.selectList(
                new LambdaQueryWrapper<KnowledgeEmbedding>()
                    .eq(KnowledgeEmbedding::getKnowledgeId, otherKnowledge.getId())
                    .orderByDesc(KnowledgeEmbedding::getCreateTime)
                    .last("LIMIT 1")
            );

            if (otherEmbeddings.isEmpty()) {
                continue;
            }

            KnowledgeEmbedding otherEmbedding = otherEmbeddings.get(0);

            List<Float> otherVector = parseEmbedding(otherEmbedding.getEmbedding());
            if (otherVector == null || otherVector.isEmpty()) {
                continue;
            }

            double similarity = cosineSimilarity(currentVector, otherVector);

            if (similarity > 0.3) {
                RelationRecommendation recommendation = new RelationRecommendation();
                
                RelationRecommendation.KnowledgeNodeInfo nodeInfo = new RelationRecommendation.KnowledgeNodeInfo();
                nodeInfo.setId(otherKnowledge.getId());
                nodeInfo.setTitle(otherKnowledge.getTitle());
                nodeInfo.setSummary(otherKnowledge.getSummary());
                nodeInfo.setImportance(otherKnowledge.getImportance());
                nodeInfo.setMasteryLevel(otherKnowledge.getMasteryLevel());
                
                recommendation.setTargetKnowledge(nodeInfo);
                recommendation.setSimilarity(similarity);
                recommendation.setRecommendedType(guessRelationType(currentKnowledge, otherKnowledge));
                recommendation.setRecommendedTypeName(getRelationTypeName(recommendation.getRecommendedType()));
                
                recommendations.add(recommendation);
            }
        }

        List<RelationRecommendation> sortedRecommendations = recommendations.stream()
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("关系推荐完成，共{}个推荐，耗时{}ms", sortedRecommendations.size(), endTime - startTime);

        return sortedRecommendations;
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        int minSize = Math.min(a.size(), b.size());
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < minSize; i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }
        
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String guessRelationType(KnowledgeNode a, KnowledgeNode b) {
        String titleA = a.getTitle().toLowerCase();
        String titleB = b.getTitle().toLowerCase();
        String summaryA = a.getSummary() != null ? a.getSummary().toLowerCase() : "";
        String summaryB = b.getSummary() != null ? b.getSummary().toLowerCase() : "";
        String contentA = a.getContentMd() != null ? a.getContentMd().toLowerCase() : "";
        String contentB = b.getContentMd() != null ? b.getContentMd().toLowerCase() : "";

        String combinedA = titleA + " " + summaryA + " " + contentA;
        String combinedB = titleB + " " + summaryB + " " + contentB;

        if (combinedA.contains("包含") || combinedA.contains("组成") || combinedA.contains("构成") ||
            combinedA.contains("框架") || combinedA.contains("架构") || combinedA.contains("体系")) {
            return "contains";
        }

        if (combinedA.contains("依赖") || combinedA.contains("需要") || combinedA.contains("基于") ||
            combinedA.contains("使用") || combinedA.contains("调用")) {
            return "depends";
        }

        if (combinedA.contains("继承") || combinedA.contains("扩展") || combinedA.contains("派生")) {
            return "inherits";
        }

        if (combinedA.contains("实现") || combinedA.contains("应用") || combinedA.contains("实践")) {
            return "implements";
        }

        return "related";
    }

    private String getRelationTypeName(String relationType) {
        switch (relationType) {
            case "contains": return "包含";
            case "depends": return "依赖";
            case "inherits": return "继承";
            case "implements": return "实现";
            case "related": return "相关";
            default: return relationType;
        }
    }

    private List<Float> parseEmbedding(String embeddingJson) {
        try {
            return com.alibaba.fastjson2.JSON.parseArray(embeddingJson, Float.class);
        } catch (Exception e) {
            log.error("解析向量失败：{}", embeddingJson, e);
            return null;
        }
    }
}