package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.CacheService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ElasticsearchService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.RelationRecommendationService;
import com.secondbrain.service.VectorSearchService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    private static final String KNOWLEDGE_CACHE_PREFIX = "knowledge:";

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final CacheService cacheService;
    private final EbbinghausService ebbinghausService;
    private final VectorSearchService vectorSearchService;

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    @Autowired(required = false)
    private RelationRecommendationService relationRecommendationService;

    @Autowired(required = false)
    private KnowledgeVectorService knowledgeVectorService;

    public KnowledgeServiceImpl(KnowledgeNodeMapper knowledgeNodeMapper, CacheService cacheService, EbbinghausService ebbinghausService, VectorSearchService vectorSearchService) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.cacheService = cacheService;
        this.ebbinghausService = ebbinghausService;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, Long userId, Integer importance, Integer masteryLevel) {
        String cacheKey = String.format("%slist:%d:%d:%s:%d", KNOWLEDGE_CACHE_PREFIX, current, size, keyword != null ? keyword : "", userId);
        if (importance != null) {
            cacheKey += ":imp-" + importance;
        }
        if (masteryLevel != null) {
            cacheKey += ":mas-" + masteryLevel;
        }
        
        Page<KnowledgeNodeVO> cached = cacheService.get(cacheKey, Page.class);
        if (cached != null) {
            log.debug("命中知识列表缓存，key：{}", cacheKey);
            return cached;
        }

        Page<KnowledgeNode> page = new Page<>(current, size);

        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeNode::getUserId, userId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(KnowledgeNode::getTitle, keyword)
                    .or()
                    .like(KnowledgeNode::getSummary, keyword));
        }
        if (importance != null) {
            wrapper.eq(KnowledgeNode::getImportance, importance);
        }
        if (masteryLevel != null) {
            wrapper.eq(KnowledgeNode::getMasteryLevel, masteryLevel);
        }
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);

        Page<KnowledgeNode> resultPage = knowledgeNodeMapper.selectPage(page, wrapper);

        Page<KnowledgeNodeVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<KnowledgeNodeVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        cacheService.set(cacheKey, voPage, 30, TimeUnit.MINUTES);
        
        return voPage;
    }

    @Override
    public KnowledgeNodeVO getById(Long id, Long userId) {
        String cacheKey = KNOWLEDGE_CACHE_PREFIX + id;
        
        KnowledgeNodeVO cached = cacheService.get(cacheKey, KnowledgeNodeVO.class);
        if (cached != null) {
            log.debug("命中知识点缓存，key：{}", cacheKey);
            return cached;
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node != null && !node.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此知识点");
        }
        KnowledgeNodeVO vo = convertToVO(node);
        
        if (vo != null) {
            cacheService.set(cacheKey, vo, 30, TimeUnit.MINUTES);
        }
        
        return vo;
    }

    @Override
    public void deleteById(Long id, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("知识点不存在");
        }
        if (!node.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此知识点");
        }
        
        knowledgeNodeMapper.deleteById(id);
        String cacheKey = KNOWLEDGE_CACHE_PREFIX + id;
        cacheService.delete(cacheKey);
        
        if (elasticsearchService != null) {
            elasticsearchService.deleteKnowledgeNode(id);
        }
        
        log.info("删除知识点并清除缓存，id：{}", id);
    }

    @Override
    public void updateImportance(Long id, Integer importance, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("知识点不存在");
        }
        if (!node.getUserId().equals(userId)) {
            throw new RuntimeException("无权更新此知识点");
        }
        
        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(id);
        updateNode.setImportance(importance);
        knowledgeNodeMapper.updateById(updateNode);
        
        String cacheKey = KNOWLEDGE_CACHE_PREFIX + id;
        cacheService.delete(cacheKey);
        log.info("更新知识点重要性并清除缓存，id：{}", id);
    }

    @Override
    public void updateKnowledge(Long id, String title, String summary, String contentMd, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new RuntimeException("知识点不存在");
        }

        if (!node.getUserId().equals(userId)) {
            throw new RuntimeException("无权更新此知识点");
        }
        
        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(id);
        updateNode.setTitle(title);
        updateNode.setSummary(summary);
        updateNode.setContentMd(contentMd);
        knowledgeNodeMapper.updateById(updateNode);
        
        String cacheKey = KNOWLEDGE_CACHE_PREFIX + id;
        cacheService.delete(cacheKey);
        
        if (elasticsearchService != null) {
            KnowledgeNode updatedNode = knowledgeNodeMapper.selectById(id);
            elasticsearchService.syncKnowledgeNode(updatedNode);
        }
        
        if (knowledgeVectorService != null) {
            KnowledgeNode nodeForVector = knowledgeNodeMapper.selectById(id);
            triggerVectorGenerationAsync(nodeForVector);
        }
        
        log.info("更新知识点内容并清除缓存，id：{}", id);
    }

    @Override
    public KnowledgeNodeVO create(String title, String summary, String contentMd, Integer importance, Long userId) {
        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(userId);
        node.setTitle(title);
        node.setSummary(summary);
        node.setContentMd(contentMd);
        node.setImportance(importance != null ? importance : 3);
        node.setMasteryLevel(0);
        node.setReviewCount(0);
        node.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
        knowledgeNodeMapper.insert(node);
        
        if (elasticsearchService != null) {
            elasticsearchService.syncKnowledgeNode(node);
        }
        
        if (relationRecommendationService != null) {
            triggerRelationRecommendationAsync(node.getId(), userId);
        }
        
        if (knowledgeVectorService != null) {
            triggerVectorGenerationAsync(node);
        }
        
        log.info("创建知识点成功，id：{}，userId：{}", node.getId(), userId);
        
        return convertToVO(node);
    }

    @Async
    public void triggerRelationRecommendationAsync(Long knowledgeId, Long userId) {
        try {
            log.info("异步触发关系推荐，knowledgeId：{}，userId：{}", knowledgeId, userId);
            relationRecommendationService.recommendRelations(knowledgeId, userId);
        } catch (Exception e) {
            log.error("关系推荐失败，knowledgeId：{}，userId：{}", knowledgeId, userId, e);
        }
    }

    @Async
    public void triggerVectorGenerationAsync(KnowledgeNode node) {
        try {
            log.info("异步触发向量生成，knowledgeId：{}，title：{}", node.getId(), node.getTitle());
            knowledgeVectorService.generateAndSaveVector(node);
        } catch (Exception e) {
            log.error("向量生成失败，knowledgeId：{}", node.getId(), e);
        }
    }

    @Override
    public List<KnowledgeNodeVO> search(String keyword, Long userId) {
        log.info("关键词搜索，keyword：{}，userId：{}", keyword, userId);
        
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeNode::getUserId, userId);
        wrapper.like(KnowledgeNode::getTitle, keyword);
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);
        
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
        return nodes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId) {
        log.info("多字段搜索，keyword：{}，userId：{}", keyword, userId);
        
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeNode::getUserId, userId);
        wrapper.and(w -> w.like(KnowledgeNode::getTitle, keyword)
                .or()
                .like(KnowledgeNode::getSummary, keyword));
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);
        
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
        return nodes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK) {
        log.info("语义搜索，queryText：{}，userId：{}，topK：{}", queryText, userId, topK);
        
        try {
            List<com.secondbrain.dto.KnowledgeReference> references = vectorSearchService.searchSimilar(queryText, userId, topK);
            return references.stream()
                    .map(ref -> {
                        KnowledgeNode node = knowledgeNodeMapper.selectById(ref.getKnowledgeId());
                        if (node != null) {
                            KnowledgeNodeVO vo = convertToVO(node);
                            vo.setScore(ref.getSimilarity());
                            return vo;
                        }
                        return null;
                    })
                    .filter(vo -> vo != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量搜索失败，降级到数据库搜索，queryText：{}，userId：{}", queryText, userId, e);
            
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeNode::getUserId, userId);
            wrapper.and(w -> w.like(KnowledgeNode::getTitle, queryText)
                    .or()
                    .like(KnowledgeNode::getSummary, queryText));
            wrapper.orderByDesc(KnowledgeNode::getCreateTime);
            wrapper.last("LIMIT " + topK);
            
            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
            return nodes.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        }
    }

    private KnowledgeNodeVO convertToVO(KnowledgeNode node) {
        if (node == null) {
            return null;
        }
        KnowledgeNodeVO vo = new KnowledgeNodeVO();
        BeanUtils.copyProperties(node, vo);
        return vo;
    }

    @Override
    public long countByUserId(Long userId) {
        return knowledgeNodeMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
        );
    }

    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return knowledgeNodeMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getUserId, userId)
                .ge(KnowledgeNode::getCreateTime, startTime)
                .lt(KnowledgeNode::getCreateTime, endTime)
        );
    }

    @Override
    public void syncToElasticsearch(Long userId) {
        if (elasticsearchService == null) {
            log.warn("Elasticsearch服务未启用，跳过同步");
            return;
        }

        try {
            log.info("开始同步用户{}的知识点到Elasticsearch", userId);
            
            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
            );

            int successCount = 0;
            int failCount = 0;
            
            for (KnowledgeNode node : nodes) {
                try {
                    elasticsearchService.syncKnowledgeNode(node);
                    successCount++;
                } catch (Exception e) {
                    log.error("同步知识点{}失败", node.getId(), e);
                    failCount++;
                }
            }
            
            log.info("同步完成，成功：{}，失败：{}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("同步知识点到Elasticsearch失败", e);
        }
    }
}
