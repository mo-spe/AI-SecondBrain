package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.*;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeRelation;
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
        log.info("自动生成知识关系，userId：{}", userId);

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
        );

        int relationsCreated = 0;
        int relationsSkipped = 0;

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                KnowledgeNode node1 = nodes.get(i);
                KnowledgeNode node2 = nodes.get(j);

                double similarity = calculateSimilarity(node1, node2);
                
                if (similarity > 0.2) {
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
                    } else {
                        relationsSkipped++;
                    }
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

    private double calculateSimilarity(KnowledgeNode node1, KnowledgeNode node2) {
        String title1 = node1.getTitle().toLowerCase();
        String title2 = node2.getTitle().toLowerCase();
        String summary1 = node1.getSummary() != null ? node1.getSummary().toLowerCase() : "";
        String summary2 = node2.getSummary() != null ? node2.getSummary().toLowerCase() : "";

        int commonWords = 0;
        String[] words1 = (title1 + " " + summary1).split("\\s+");
        String[] words2 = (title2 + " " + summary2).split("\\s+");

        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 2) {
                    commonWords++;
                    break;
                }
            }
        }

        int totalWords = Math.max(words1.length, words2.length);
        return totalWords > 0 ? (double) commonWords / totalWords : 0.0;
    }
}