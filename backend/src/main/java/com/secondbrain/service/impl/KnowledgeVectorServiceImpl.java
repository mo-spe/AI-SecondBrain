package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.ElasticsearchService;
import com.secondbrain.service.EmbeddingService;
import com.secondbrain.service.KnowledgeVectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识向量服务实现类.
 * <p>负责为知识节点生成、保存和管理向量嵌入</p>
 */
@Service
public class KnowledgeVectorServiceImpl implements KnowledgeVectorService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorServiceImpl.class);

    private static final String DEFAULT_MODEL = "text-embedding-v2";

    private final EmbeddingService embeddingService;
    private final KnowledgeEmbeddingMapper embeddingMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ElasticsearchService elasticsearchService;

    public KnowledgeVectorServiceImpl(EmbeddingService embeddingService, KnowledgeEmbeddingMapper embeddingMapper,
                                      KnowledgeNodeMapper knowledgeNodeMapper,
                                      @Autowired(required = false) ElasticsearchService elasticsearchService) {
        this.embeddingService = embeddingService;
        this.embeddingMapper = embeddingMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.elasticsearchService = elasticsearchService;
    }

    /**
     * 为单个知识节点生成并保存向量.
     *
     * @param node 知识节点
     * @return void
     */
    @Override
    @Async("vectorTaskExecutor")
    @Transactional
    public void generateAndSaveVector(KnowledgeNode node) {
        Long userId = node.getUserId();
        generateAndSaveVector(node, userId);
    }
    
    private void generateAndSaveVector(KnowledgeNode node, Long userId) {
        try {
            log.info("开始为知识节点生成向量，nodeId：{}，title：{}，userId：{}", node.getId(), node.getTitle(), userId);

            String contentForEmbedding = prepareContentForEmbedding(node);
            
            List<Float> embedding = embeddingService.generateEmbedding(contentForEmbedding, userId);

            if (embedding == null || embedding.isEmpty()) {
                log.warn("向量生成失败，nodeId：{}", node.getId());
                return;
            }

            KnowledgeEmbedding knowledgeEmbedding = new KnowledgeEmbedding();
            knowledgeEmbedding.setKnowledgeId(node.getId());
            knowledgeEmbedding.setContent(contentForEmbedding);
            knowledgeEmbedding.setEmbedding(JSON.toJSONString(embedding));
            knowledgeEmbedding.setModel("text-embedding-v2");

            KnowledgeEmbedding existing = embeddingMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeEmbedding>()
                            .eq(KnowledgeEmbedding::getKnowledgeId, node.getId())
            );

            if (existing != null) {
                existing.setEmbedding(JSON.toJSONString(embedding));
                embeddingMapper.updateById(existing);
                log.info("更新已有向量，nodeId：{}", node.getId());
            } else {
                embeddingMapper.insert(knowledgeEmbedding);
                log.info("保存新向量，nodeId：{}", node.getId());
            }

            if (elasticsearchService != null) {
                elasticsearchService.syncKnowledgeNode(node);
                log.info("同步到 Elasticsearch，nodeId：{}", node.getId());
            }

            log.info("向量生成完成，nodeId：{}，维度：{}", node.getId(), embedding.size());

        } catch (Exception e) {
            log.error("生成向量失败，nodeId：{}", node.getId(), e);
        }
    }

    /**
     * 批量为用户生成向量.
     *
     * @param userId 用户ID
     * @return void
     */
    @Override
    @Async("vectorTaskExecutor")
    public void batchGenerateVectors(Long userId) {
        try {
            log.info("开始批量生成向量，userId：{}", userId);

            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNode>()
                            .eq(KnowledgeNode::getUserId, userId)
                            .eq(KnowledgeNode::getDeleted, 0)
            );

            log.info("找到{}个知识节点需要生成向量", nodes.size());

            int successCount = 0;
            int failCount = 0;

            for (KnowledgeNode node : nodes) {
                try {
                    generateAndSaveVector(node, userId);
                    successCount++;
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("生成向量失败，nodeId：{}", node.getId(), e);
                    failCount++;
                }
            }

            log.info("批量向量生成完成，成功：{}，失败：{}", successCount, failCount);

        } catch (Exception e) {
            log.error("批量生成向量失败，userId：{}", userId, e);
        }
    }

    /**
     * 重新生成指定知识节点的向量.
     *
     * @param knowledgeId 知识节点ID
     * @return void
     */
    @Override
    @Async("vectorTaskExecutor")
    @Transactional
    public void regenerateVector(Long knowledgeId) {
        try {
            log.info("开始重新生成向量，knowledgeId：{}", knowledgeId);

            KnowledgeNode node = knowledgeNodeMapper.selectById(knowledgeId);
            if (node == null) {
                log.warn("知识节点不存在，knowledgeId：{}", knowledgeId);
                return;
            }

            generateAndSaveVector(node);

            log.info("向量重新生成完成，knowledgeId：{}", knowledgeId);

        } catch (Exception e) {
            log.error("重新生成向量失败，knowledgeId：{}", knowledgeId, e);
        }
    }

    private String prepareContentForEmbedding(KnowledgeNode node) {
        StringBuilder content = new StringBuilder();
        content.append(node.getTitle()).append("\n");
        
        if (node.getSummary() != null && !node.getSummary().isEmpty()) {
            content.append(node.getSummary()).append("\n");
        }
        
        if (node.getContentMd() != null && !node.getContentMd().isEmpty()) {
            content.append(node.getContentMd());
        }
        
        return content.toString();
    }
}
