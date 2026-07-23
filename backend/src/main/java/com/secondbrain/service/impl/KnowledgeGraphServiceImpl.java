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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识图谱服务实现类.
 * <p>提供知识图谱的构建、关系管理及自动关系生成功能</p>
 */
@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphServiceImpl.class);

    private final KnowledgeRelationMapper relationMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final KnowledgeEmbeddingMapper embeddingMapper;
    private final VectorSearchService vectorSearchService;

    public KnowledgeGraphServiceImpl(KnowledgeRelationMapper relationMapper, KnowledgeNodeMapper knowledgeNodeMapper,
                                     KnowledgeEmbeddingMapper embeddingMapper, VectorSearchService vectorSearchService) {
        this.relationMapper = relationMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.embeddingMapper = embeddingMapper;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * 获取知识图谱.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 知识图谱（节点和边）
     */
    @Override
    public KnowledgeGraph getGraph(Long userId, Long workspaceId) {
        log.info("获取知识图谱，userId：{}，workspaceId：{}", userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> nodeWrapper = new LambdaQueryWrapper<>();
        applyNodeFilter(nodeWrapper, userId, workspaceId);
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(nodeWrapper);

        LambdaQueryWrapper<KnowledgeRelation> relWrapper = new LambdaQueryWrapper<>();
        applyRelationFilter(relWrapper, userId, workspaceId);
        List<KnowledgeRelation> relations = relationMapper.selectList(relWrapper);

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

    /**
     * 添加知识关系.
     *
     * @param request     关系请求（源节点、目标节点、关系类型等）
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void addRelation(KnowledgeRelationRequest request, Long userId, Long workspaceId) {
        log.info("添加知识关系，fromId：{}，toId：{}，type：{}",
            request.getSourceId(), request.getTargetId(), request.getRelationType());

        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setUserId(userId);
        relation.setWorkspaceId(workspaceId);
        relation.setFromKnowledgeId(request.getSourceId());
        relation.setToKnowledgeId(request.getTargetId());
        relation.setRelationType(request.getRelationType());
        relation.setRelationName(request.getRelationName());
        relation.setWeight(request.getRelationStrength() != null ? Double.valueOf(request.getRelationStrength()) : 1.0);

        relationMapper.insert(relation);

        log.info("知识关系添加成功，id：{}", relation.getId());
    }

    /**
     * 删除知识关系.
     *
     * @param relationId  关系ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void deleteRelation(Long relationId, Long userId, Long workspaceId) {
        log.info("删除知识关系，id：{}，userId：{}，workspaceId：{}", relationId, userId, workspaceId);

        KnowledgeRelation relation = relationMapper.selectById(relationId);
        if (relation != null && hasRelationAccess(relation, userId, workspaceId)) {
            relationMapper.deleteById(relationId);
            log.info("知识关系删除成功");
        } else {
            throw new IllegalStateException("关系不存在或无权限删除");
        }
    }

    /**
     * 自动生成知识关系.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void autoGenerateRelations(Long userId, Long workspaceId) {
        log.info("自动生成知识关系（使用向量嵌入），userId：{}，workspaceId：{}", userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> nodeWrapper = new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getDeleted, 0);
        applyNodeFilter(nodeWrapper, userId, workspaceId);
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(nodeWrapper);

        if (nodes.isEmpty()) {
            log.warn("用户{}没有知识点，无法生成关系", userId);
            return;
        }

        if (nodes.size() < 2) {
            log.warn("用户{}知识点数量不足，需要至少 2 个知识点才能生成关系", userId);
            return;
        }

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

                Double similarity = calculateVectorSimilarity(node1, node2, embeddingMap);
                if (similarity == null) {
                    similarity = calculateSimilarity(node1, node2);
                }

                if (similarity > 0.3) {
                    LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
                    applyRelationFilter(wrapper, userId, workspaceId);
                    wrapper.eq(KnowledgeRelation::getFromKnowledgeId, node1.getId());
                    wrapper.eq(KnowledgeRelation::getToKnowledgeId, node2.getId());

                    if (relationMapper.selectCount(wrapper) == 0) {
                        KnowledgeRelation relation = new KnowledgeRelation();
                        relation.setUserId(userId);
                        relation.setWorkspaceId(workspaceId);
                        relation.setFromKnowledgeId(node1.getId());
                        relation.setToKnowledgeId(node2.getId());
                        relation.setRelationType("related");
                        relation.setRelationName("相关");
                        relation.setWeight(similarity * 5);
                        relationMapper.insert(relation);
                        relationsCreated++;
                        log.info("创建关系：{} -> {}，相似度：{:.3f}", node1.getTitle(), node2.getTitle(), similarity);
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

    /**
     * 应用节点过滤条件.
     * workspaceId 为 null 时降级为 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyNodeFilter(LambdaQueryWrapper<KnowledgeNode> wrapper, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            wrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(KnowledgeNode::getUserId, userId);
        }
    }

    /**
     * 应用关系过滤条件.
     * workspaceId 为 null 时降级为 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyRelationFilter(LambdaQueryWrapper<KnowledgeRelation> wrapper, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            wrapper.eq(KnowledgeRelation::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(KnowledgeRelation::getUserId, userId);
        }
    }

    /**
     * 检查关系访问权限.
     * workspaceId 不为 null 时按工作区校验，否则按 userId 校验.
     */
    private boolean hasRelationAccess(KnowledgeRelation relation, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            return workspaceId.equals(relation.getWorkspaceId());
        }
        return relation.getUserId().equals(userId);
    }

    private int calculateNodeSize(Integer importance) {
        if (importance == null) return 30;
        return 20 + importance * 5;
    }

    private String calculateNodeColor(Integer masteryLevel) {
        if (masteryLevel == null) return "#909399";
        return switch (masteryLevel) {
            case 5 -> "#67C23A";
            case 4 -> "#95D475";
            case 3 -> "#E6A23C";
            case 2 -> "#F56C6C";
            case 1 -> "#F89898";
            default -> "#909399";
        };
    }

    private String getRelationTypeLabel(String relationType, String relationName) {
        if (relationName != null && !relationName.isEmpty()) {
            return relationName;
        }
        return switch (relationType) {
            case "contains" -> "包含";
            case "depends" -> "依赖";
            case "related" -> "相关";
            case "inherits" -> "继承";
            case "implements" -> "实现";
            default -> relationType;
        };
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

            return vectorSearchService.calculateSimilarity(vector1, vector2);
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

        return titleSimilarity * 0.6 + contentSimilarity * 0.4;
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
