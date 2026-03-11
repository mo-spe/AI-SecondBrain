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
import com.secondbrain.vo.KnowledgeNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private ElasticsearchService elasticsearchService;

    public KnowledgeServiceImpl(KnowledgeNodeMapper knowledgeNodeMapper, CacheService cacheService, EbbinghausService ebbinghausService) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.cacheService = cacheService;
        this.ebbinghausService = ebbinghausService;
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
        
        log.info("创建知识点成功，id：{}，userId：{}", node.getId(), userId);
        
        return convertToVO(node);
    }

    @Override
    public List<KnowledgeNodeVO> search(String keyword, Long userId) {
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
    public List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId) {
        if (elasticsearchService != null) {
            try {
                List<KnowledgeDocument> documents = elasticsearchService.multiFieldSearch(keyword, userId);
                return documents.stream()
                        .map(doc -> {
                            KnowledgeNode node = knowledgeNodeMapper.selectById(doc.getId());
                            return convertToVO(node);
                        })
                        .filter(vo -> vo != null)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Elasticsearch多字段搜索失败，降级到数据库搜索，keyword：{}，userId：{}", keyword, userId, e);
            }
        }
        
        log.warn("Elasticsearch未配置，降级到数据库搜索，keyword：{}，userId：{}", keyword, userId);
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeNode::getUserId, userId);
        wrapper.and(w -> w.like(KnowledgeNode::getTitle, keyword)
                .or()
                .like(KnowledgeNode::getSummary, keyword)
                .or()
                .like(KnowledgeNode::getContentMd, keyword));
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);
        
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
        return nodes.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK) {
        if (elasticsearchService != null) {
            try {
                List<KnowledgeDocument> documents = elasticsearchService.semanticSearch(queryText, userId, topK);
                return documents.stream()
                        .map(doc -> {
                            KnowledgeNode node = knowledgeNodeMapper.selectById(doc.getId());
                            return convertToVO(node);
                        })
                        .filter(vo -> vo != null)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Elasticsearch语义搜索失败，降级到数据库搜索，queryText：{}，userId：{}", queryText, userId, e);
            }
        }
        
        log.warn("Elasticsearch未配置，降级到数据库搜索，queryText：{}，userId：{}", queryText, userId);
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
}
