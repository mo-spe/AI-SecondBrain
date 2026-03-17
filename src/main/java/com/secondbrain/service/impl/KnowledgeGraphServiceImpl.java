package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.*;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeRelation;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeRelationMapper;
import com.secondbrain.service.KnowledgeGraphService;
import com.secondbrain.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphServiceImpl.class);

    @Autowired
    private KnowledgeRelationMapper relationMapper;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private KnowledgeEmbeddingMapper embeddingMapper;

    @Autowired
    private VectorSearchService vectorSearchService;

    @Override
    public KnowledgeGraph getGraph(Long userId) {
        log.info("获取知识图谱，userId：{}", userId);

        List<KnowledgeRelation> relations = relationMapper.findByUserId(userId);
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
        );

        Map<Long, KnowledgeNode> nodeMap = allNodes.stream()
            .collect(Collectors.toMap(KnowledgeNode::getId, node -> node));

        KnowledgeGraph graph = new KnowledgeGraph();
        graph.setNodes(new ArrayList<>());
        graph.setEdges(new ArrayList<>());

        for (KnowledgeNode node : allNodes) {
            GraphNode graphNode = new GraphNode();
            graphNode.setId(String.valueOf(node.getId()));
            graphNode.setLabel(node.getTitle());
            graphNode.setType("knowledge");
            graphNode.setImportance(node.getImportance());
            graphNode.setMasteryLevel(node.getMasteryLevel());
            graphNode.setSize(calculateNodeSize(node.getImportance()));
            graphNode.setColor(calculateNodeColor(node.getMasteryLevel()));
            graph.getNodes().add(graphNode);
        }

        for (KnowledgeRelation relation : relations) {
            GraphEdge edge = new GraphEdge();
            edge.setSource(String.valueOf(relation.getFromKnowledgeId()));
            edge.setTarget(String.valueOf(relation.getToKnowledgeId()));
            edge.setLabel(getRelationTypeLabel(relation.getRelationType(), relation.getRelationName()));
            edge.setStrength(relation.getWeight() != null ? relation.getWeight().intValue() : 1);
            graph.getEdges().add(edge);
        }

        log.info("知识图谱构建完成，节点数：{}，边数：{}", graph.getNodes().size(), graph.getEdges().size());

        return graph;
    }

    @Override
    public void addRelation(KnowledgeRelationRequest request, Long userId) {
        log.info("添加知识关系，fromId：{}，toId：{}，type：{}", 
            request.getSourceId(), request.getTargetId(), request.getRelationType());

        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setUserId(userId);
        relation.setFromKnowledgeId(request.getSourceId());
        relation.setToKnowledgeId(request.getTargetId());
        relation.setRelationType(request.getRelationType());
        relation.setRelationName(request.getRelationName());
        relation.setWeight(request.getRelationStrength() != null ? Double.valueOf(request.getRelationStrength()) : 1.0);

        relationMapper.insert(relation);

        log.info("知识关系添加成功，id：{}", relation.getId());
    }

    @Override
    public void deleteRelation(Long relationId, Long userId) {
        log.info("删除知识关系，id：{}，userId：{}", relationId, userId);

        KnowledgeRelation relation = relationMapper.selectById(relationId);
        if (relation != null && relation.getUserId().equals(userId)) {
            relationMapper.deleteById(relationId);
            log.info("知识关系删除成功");
        } else {
            throw new RuntimeException("关系不存在或无权限删除");
        }
    }

    @Override
    public void autoGenerateRelations(Long userId) {
        log.info("自动生成知识关系（使用向量嵌入），userId：{}", userId);

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .eq(KnowledgeNode::getDeleted, 0)
        );

        if (nodes.isEmpty()) {
            log.warn("用户{}没有知识点，无法生成关系", userId);
            return;
        }

        if (nodes.size() < 2) {
            log.warn("用户{}知识点数量不足，需要至少 2 个知识点才能生成关系", userId);
            return;
        }

        // 获取所有知识节点的向量嵌入
        Map<Long, KnowledgeEmbedding> embeddingMap = nodes.stream()
            .map(node -> embeddingMapper.getByKnowledgeId(node.getId()))
            .filter(embedding -> embedding != null && embedding.getEmbedding() != null)
            .collect(Collectors.toMap(
                embedding -> embedding.getKnowledgeId(),
                embedding -> embedding
            ));

        log.info("找到{}个知识节点，其中{}个有向量嵌入", nodes.size(), embeddingMap.size());

        if (embeddingMap.size() < 2) {
            log.warn("有向量嵌入的节点不足 2 个，无法使用向量相似度生成关系");
            return;
        }

        int relationsCreated = 0;
        int relationsSkipped = 0;
        int totalPairs = nodes.size() * (nodes.size() - 1) / 2;
        int processedPairs = 0;

        log.info("开始处理{}个知识点，共{}对组合", nodes.size(), totalPairs);

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                processedPairs++;
                KnowledgeNode node1 = nodes.get(i);
                KnowledgeNode node2 = nodes.get(j);

                // 优先使用向量嵌入计算相似度
                Double similarity = calculateVectorSimilarity(node1, node2, embeddingMap);
                
                // 如果没有向量嵌入，使用文本相似度作为后备
                if (similarity == null) {
                    similarity = calculateSimilarity(node1, node2);
                    log.debug("使用文本相似度：{} vs {} = {:.3f}", node1.getTitle(), node2.getTitle(), similarity);
                } else {
                    log.debug("使用向量相似度：{} vs {} = {:.3f}", node1.getTitle(), node2.getTitle(), similarity);
                }
                
                if (similarity > 0.3) { // 提高阈值到 0.3，因为向量相似度更准确
                    KnowledgeRelation relation = new KnowledgeRelation();
                    relation.setUserId(userId);
                    relation.setFromKnowledgeId(node1.getId());
                    relation.setToKnowledgeId(node2.getId());
                    relation.setRelationType("related");
                    relation.setRelationName("相关");
                    relation.setWeight(similarity * 5);

                    LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(KnowledgeRelation::getUserId, userId);
                    wrapper.eq(KnowledgeRelation::getFromKnowledgeId, node1.getId());
                    wrapper.eq(KnowledgeRelation::getToKnowledgeId, node2.getId());
                    
                    if (relationMapper.selectCount(wrapper) == 0) {
                        relationMapper.insert(relation);
                        relationsCreated++;
                        log.info("创建关系：{} -> {}，向量相似度：{:.3f}", node1.getTitle(), node2.getTitle(), similarity);
                    } else {
                        relationsSkipped++;
                    }
                }
                
                if (processedPairs % 100 == 0) {
                    log.info("已处理 {}/{} 对组合", processedPairs, totalPairs);
                }
            }
        }

        log.info("自动生成知识关系完成，创建：{}，跳过：{}", relationsCreated, relationsSkipped);
    }

    private int calculateNodeSize(Integer importance) {
        if (importance == null) return 30;
        return 20 + importance * 5;
    }

    private String calculateNodeColor(Integer masteryLevel) {
        if (masteryLevel == null) return "#909399";
        
        switch (masteryLevel) {
            case 5: return "#67C23A";
            case 4: return "#95D475";
            case 3: return "#E6A23C";
            case 2: return "#F56C6C";
            case 1: return "#F89898";
            default: return "#909399";
        }
    }

    private String getRelationTypeLabel(String relationType, String relationName) {
        if (relationName != null && !relationName.isEmpty()) {
            return relationName;
        }
        
        switch (relationType) {
            case "contains": return "包含";
            case "depends": return "依赖";
            case "related": return "相关";
            case "inherits": return "继承";
            case "implements": return "实现";
            default: return relationType;
        }
    }

    private Double calculateVectorSimilarity(KnowledgeNode node1, KnowledgeNode node2, 
                                              Map<Long, KnowledgeEmbedding> embeddingMap) {
        KnowledgeEmbedding embedding1 = embeddingMap.get(node1.getId());
        KnowledgeEmbedding embedding2 = embeddingMap.get(node2.getId());
        
        if (embedding1 == null || embedding2 == null) {
            return null;
        }
        
        try {
            List<Float> vector1 = JSON.parseArray(embedding1.getEmbedding(), Float.class);
            List<Float> vector2 = JSON.parseArray(embedding2.getEmbedding(), Float.class);
            
            if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
                return null;
            }
            
            double similarity = vectorSearchService.calculateSimilarity(vector1, vector2);
            log.debug("向量余弦相似度：{} vs {} = {:.6f}", node1.getTitle(), node2.getTitle(), similarity);
            
            return similarity;
        } catch (Exception e) {
            log.warn("解析向量嵌入失败，nodeId: {}, {}", node1.getId(), node2.getId(), e);
            return null;
        }
    }

    private double calculateSimilarity(KnowledgeNode node1, KnowledgeNode node2) {
        String title1 = node1.getTitle();
        String title2 = node2.getTitle();
        String summary1 = node1.getSummary() != null ? node1.getSummary() : "";
        String summary2 = node2.getSummary() != null ? node2.getSummary() : "";
        String content1 = node1.getContentMd() != null ? node1.getContentMd() : "";
        String content2 = node2.getContentMd() != null ? node2.getContentMd() : "";

        String text1 = title1 + " " + summary1 + " " + content1;
        String text2 = title2 + " " + summary2 + " " + content2;

        double titleSimilarity = calculateTextSimilarity(title1, title2);
        double contentSimilarity = calculateTextSimilarity(text1, text2);

        double combinedSimilarity = titleSimilarity * 0.6 + contentSimilarity * 0.4;
        
        log.debug("相似度计算：标题相似度={:.3f}，内容相似度={:.3f}，综合相似度={:.3f}", 
                 titleSimilarity, contentSimilarity, combinedSimilarity);
        
        return combinedSimilarity;
    }

    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text1.isEmpty() || text2 == null || text2.isEmpty()) {
            return 0.0;
        }

        String normalized1 = normalizeText(text1);
        String normalized2 = normalizeText(text2);

        if (normalized1.isEmpty() || normalized2.isEmpty()) {
            return 0.0;
        }

        double jaccardSimilarity = calculateJaccardSimilarity(normalized1, normalized2);
        double commonPhraseSimilarity = calculateCommonPhraseSimilarity(normalized1, normalized2);

        return jaccardSimilarity * 0.7 + commonPhraseSimilarity * 0.3;
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
                .replaceAll("[\\p{Punct}\\s\\d]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double calculateJaccardSimilarity(String text1, String text2) {
        String[] words1 = extractWords(text1);
        String[] words2 = extractWords(text2);

        if (words1.length == 0 || words2.length == 0) {
            return 0.0;
        }

        int intersection = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    intersection++;
                    break;
                }
            }
        }

        int union = words1.length + words2.length - intersection;
        return union > 0 ? (double) intersection / union : 0.0;
    }

    private double calculateCommonPhraseSimilarity(String text1, String text2) {
        String[] phrases1 = extractPhrases(text1);
        String[] phrases2 = extractPhrases(text2);

        if (phrases1.length == 0 || phrases2.length == 0) {
            return 0.0;
        }

        int commonPhrases = 0;
        for (String phrase1 : phrases1) {
            for (String phrase2 : phrases2) {
                if (phrase1.equals(phrase2)) {
                    commonPhrases++;
                    break;
                }
            }
        }

        return (double) commonPhrases / Math.max(phrases1.length, phrases2.length);
    }

    private String[] extractWords(String text) {
        return text.split("\\s+");
    }

    private String[] extractPhrases(String text) {
        String[] words = text.split("\\s+");
        java.util.Set<String> phrases = new java.util.HashSet<>();
        
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].length() > 1 && words[i + 1].length() > 1) {
                phrases.add(words[i] + words[i + 1]);
            }
        }
        
        return phrases.toArray(new String[0]);
    }
}