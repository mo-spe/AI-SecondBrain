package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.KnowledgeReference;
import com.secondbrain.elasticsearch.KnowledgeDocument;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeNodeTagRelation;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeNodeTagRelationMapper;
import com.secondbrain.service.CacheService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ElasticsearchService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.GamificationService;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.KnowledgeTagService;
import com.secondbrain.service.RelationRecommendationService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.VectorSearchService;
import com.secondbrain.vo.KnowledgeNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识节点服务实现.
 * <p>提供知识点的增删改查、搜索、缓存管理及Elasticsearch同步等功能</p>
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    private static final String KNOWLEDGE_CACHE_PREFIX = "knowledge:";

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final CacheService cacheService;
    private final EbbinghausService ebbinghausService;
    private final VectorSearchService vectorSearchService;
    private final ElasticsearchService elasticsearchService;
    private final RelationRecommendationService relationRecommendationService;
    private final KnowledgeVectorService knowledgeVectorService;
    private final GamificationService gamificationService;
    private final KnowledgeTagService knowledgeTagService;
    private final KnowledgeNodeTagRelationMapper knowledgeNodeTagRelationMapper;
    private final ReviewCardService reviewCardService;

    @Autowired
    public KnowledgeServiceImpl(KnowledgeNodeMapper knowledgeNodeMapper,
                                CacheService cacheService,
                                EbbinghausService ebbinghausService,
                                VectorSearchService vectorSearchService,
                                @Autowired(required = false) ElasticsearchService elasticsearchService,
                                @Autowired(required = false) RelationRecommendationService relationRecommendationService,
                                @Autowired(required = false) KnowledgeVectorService knowledgeVectorService,
                                GamificationService gamificationService,
                                KnowledgeTagService knowledgeTagService,
                                KnowledgeNodeTagRelationMapper knowledgeNodeTagRelationMapper,
                                ReviewCardService reviewCardService) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.cacheService = cacheService;
        this.ebbinghausService = ebbinghausService;
        this.vectorSearchService = vectorSearchService;
        this.elasticsearchService = elasticsearchService;
        this.relationRecommendationService = relationRecommendationService;
        this.knowledgeVectorService = knowledgeVectorService;
        this.gamificationService = gamificationService;
        this.knowledgeTagService = knowledgeTagService;
        this.knowledgeNodeTagRelationMapper = knowledgeNodeTagRelationMapper;
        this.reviewCardService = reviewCardService;
    }

    /**
     * 分页查询知识节点列表.
     *
     * @param current       当前页
     * @param size          每页数量
     * @param keyword       关键词
     * @param userId        用户ID
     * @param importance    重要性
     * @param masteryLevel  掌握程度
     * @param workspaceId   工作区ID
     * @return 知识节点分页结果
     */
    @Override
    public Page<KnowledgeNodeVO> list(Integer current, Integer size, String keyword, Long userId, Integer importance, Integer masteryLevel, Long workspaceId, Long tagId, Integer needReview) {
        Page<KnowledgeNode> page = new Page<>(current, size);

        LambdaQueryWrapper<KnowledgeNode> wrapper = buildBaseWrapper(userId, workspaceId);
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
        if (needReview != null) {
            wrapper.eq(KnowledgeNode::getNeedReview, needReview);
        }
        if (tagId != null) {
            List<Long> nodeIds = knowledgeNodeTagRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                    .eq(KnowledgeNodeTagRelation::getTagId, tagId)
            ).stream()
                .map(KnowledgeNodeTagRelation::getNodeId)
                .distinct()
                .collect(Collectors.toList());

            if (nodeIds.isEmpty()) {
                Page<KnowledgeNodeVO> emptyPage = new Page<>();
                emptyPage.setCurrent(current);
                emptyPage.setSize(size);
                emptyPage.setTotal(0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            wrapper.in(KnowledgeNode::getId, nodeIds);
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

        return voPage;
    }

    /**
     * 根据ID获取知识节点.
     *
     * @param id          知识节点ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点VO
     */
    @Override
    public KnowledgeNodeVO getById(Long id, Long userId, Long workspaceId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node != null && !hasAccess(node, userId, workspaceId)) {
            throw new IllegalStateException("无权访问此知识点");
        }
        return convertToVO(node);
    }

    /**
     * 根据ID删除知识节点.
     *
     * @param id          知识节点ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void deleteById(Long id, Long userId, Long workspaceId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new IllegalStateException("知识点不存在");
        }
        if (!hasAccess(node, userId, workspaceId)) {
            throw new IllegalStateException("无权删除此知识点");
        }

        knowledgeNodeMapper.deleteById(id);

        if (elasticsearchService != null) {
            elasticsearchService.deleteKnowledgeNode(id);
        }

        log.info("删除知识点，id：{}", id);
    }

    /**
     * 更新知识节点重要性.
     *
     * @param id          知识节点ID
     * @param importance  重要性
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void updateImportance(Long id, Integer importance, Long userId, Long workspaceId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new IllegalStateException("知识点不存在");
        }
        if (!hasAccess(node, userId, workspaceId)) {
            throw new IllegalStateException("无权更新此知识点");
        }

        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(id);
        updateNode.setImportance(importance);
        knowledgeNodeMapper.updateById(updateNode);

        log.info("更新知识点重要性，id：{}", id);
    }

    @Override
    public void toggleNeedReview(Long id, Integer needReview, Long userId, Long workspaceId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new IllegalStateException("知识点不存在");
        }
        if (!hasAccess(node, userId, workspaceId)) {
            throw new IllegalStateException("无权操作此知识点");
        }

        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(id);
        updateNode.setNeedReview(needReview);

        if (needReview != null && needReview == 1) {
            // 纳入复习时重置掌握程度和复习计数
            updateNode.setMasteryLevel(0);
            updateNode.setReviewCount(0);
            updateNode.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
            knowledgeNodeMapper.updateById(updateNode);

            // 为该知识点生成初始复习卡片
            try {
                List<com.secondbrain.entity.ReviewCard> existingCards = reviewCardService.getReviewCardsByNodeId(id, userId, workspaceId);
                if (existingCards == null || existingCards.isEmpty()) {
                    reviewCardService.generateReviewCard(id, "choice", "auto", userId);
                    reviewCardService.generateReviewCard(id, "choice", "auto", userId);
                    log.info("纳入复习目标并生成复习卡片，nodeId={}", id);
                }
            } catch (Exception e) {
                log.error("纳入复习目标时生成卡片失败，nodeId={}", id, e);
            }
        } else {
            // 取消复习时清理该知识点的复习卡片
            knowledgeNodeMapper.updateById(updateNode);
            try {
                List<com.secondbrain.entity.ReviewCard> cards = reviewCardService.getReviewCardsByNodeId(id, userId, workspaceId);
                if (cards != null) {
                    for (com.secondbrain.entity.ReviewCard card : cards) {
                        reviewCardService.deleteReviewCard(card.getId(), userId);
                    }
                    log.info("取消复习目标并删除{}张复习卡片，nodeId={}", cards.size(), id);
                }
            } catch (Exception e) {
                log.error("取消复习目标时清理卡片失败，nodeId={}", id, e);
            }
        }

        log.info("切换知识点复习目标状态，id={}，needReview={}", id, needReview);
    }

    /**
     * 更新知识节点内容.
     *
     * @param id          知识节点ID
     * @param title       标题
     * @param summary     摘要
     * @param contentMd   Markdown内容
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void updateKnowledge(Long id, String title, String summary, String contentMd, Long userId, Long workspaceId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(id);
        if (node == null) {
            throw new IllegalStateException("知识点不存在");
        }

        if (!hasAccess(node, userId, workspaceId)) {
            throw new IllegalStateException("无权更新此知识点");
        }

        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(id);
        updateNode.setTitle(title);
        updateNode.setSummary(summary);
        updateNode.setContentMd(contentMd);
        knowledgeNodeMapper.updateById(updateNode);

        if (elasticsearchService != null) {
            KnowledgeNode updatedNode = knowledgeNodeMapper.selectById(id);
            elasticsearchService.syncKnowledgeNode(updatedNode);
        }

        if (knowledgeVectorService != null) {
            KnowledgeNode nodeForVector = knowledgeNodeMapper.selectById(id);
            triggerVectorGenerationAsync(nodeForVector);
        }

        log.info("更新知识点内容，id：{}", id);
    }

    /**
     * 创建知识节点.
     *
     * @param title       标题
     * @param summary     摘要
     * @param contentMd   Markdown内容
     * @param importance  重要性
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 创建后的知识节点VO
     */
    @Override
    public KnowledgeNodeVO create(String title, String summary, String contentMd, Integer importance, Long userId, Long workspaceId) {
        KnowledgeNode node = new KnowledgeNode();
        node.setUserId(userId);
        node.setWorkspaceId(workspaceId);
        node.setTitle(title);
        node.setSummary(summary);
        node.setContentMd(contentMd);
        node.setImportance(importance != null ? importance : 3);
        node.setMasteryLevel(0);
        node.setReviewCount(0);
        node.setNextReviewTime(ebbinghausService.calculateNextReviewTime(LocalDateTime.now(), 0, true));
        knowledgeNodeMapper.insert(node);

        // 游戏化积分奖励 — 失败不影响知识创建主流程
        gamificationService.awardCreatePoints(userId, node.getId());

        if (elasticsearchService != null) {
            elasticsearchService.syncKnowledgeNode(node);
        }

        if (relationRecommendationService != null) {
            triggerRelationRecommendationAsync(node.getId(), userId);
        }

        if (knowledgeVectorService != null) {
            triggerVectorGenerationAsync(node);
        }

        log.info("创建知识点成功，id：{}，userId：{}，workspaceId：{}", node.getId(), userId, workspaceId);

        return convertToVO(node);
    }

    /**
     * 异步触发关系推荐.
     *
     * @param knowledgeId 知识节点ID
     * @param userId      用户ID
     * @return void
     */
    @Async
    public void triggerRelationRecommendationAsync(Long knowledgeId, Long userId) {
        try {
            log.info("异步触发关系推荐，knowledgeId：{}，userId：{}", knowledgeId, userId);
            relationRecommendationService.recommendRelations(knowledgeId, userId);
        } catch (Exception e) {
            log.error("关系推荐失败，knowledgeId：{}，userId：{}", knowledgeId, userId, e);
        }
    }

    /**
     * 异步触发向量生成.
     *
     * @param node 知识节点
     * @return void
     */
    @Async
    public void triggerVectorGenerationAsync(KnowledgeNode node) {
        try {
            log.info("异步触发向量生成，knowledgeId：{}，title：{}", node.getId(), node.getTitle());
            knowledgeVectorService.generateAndSaveVector(node);
        } catch (Exception e) {
            log.error("向量生成失败，knowledgeId：{}", node.getId(), e);
        }
    }

    /**
     * 关键词搜索知识节点.
     *
     * @param keyword     关键词
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    @Override
    public List<KnowledgeNodeVO> search(String keyword, Long userId, Long workspaceId) {
        log.info("关键词搜索，keyword：{}，userId：{}，workspaceId：{}", keyword, userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> wrapper = buildBaseWrapper(userId, workspaceId);
        wrapper.like(KnowledgeNode::getTitle, keyword);
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
        return nodes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 多字段搜索知识节点.
     *
     * @param keyword     关键词
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    @Override
    public List<KnowledgeNodeVO> multiFieldSearch(String keyword, Long userId, Long workspaceId) {
        log.info("多字段搜索，keyword：{}，userId：{}，workspaceId：{}", keyword, userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> wrapper = buildBaseWrapper(userId, workspaceId);
        wrapper.and(w -> w.like(KnowledgeNode::getTitle, keyword)
                .or()
                .like(KnowledgeNode::getSummary, keyword));
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
        return nodes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 语义搜索知识节点.
     *
     * @param queryText   查询文本
     * @param userId      用户ID
     * @param topK        返回数量上限
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    @Override
    public List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK, Long workspaceId) {
        return semanticSearch(queryText, userId, topK, null, workspaceId);
    }

    /**
     * 语义搜索知识节点（支持用户自定义API Key）.
     *
     * @param queryText   查询文本
     * @param userId      用户ID
     * @param topK        返回数量上限
     * @param userApiKey  用户API Key
     * @param workspaceId 工作区ID
     * @return 知识节点列表
     */
    @Override
    public List<KnowledgeNodeVO> semanticSearch(String queryText, Long userId, int topK, String userApiKey, Long workspaceId) {
        log.info("语义搜索，queryText：{}，userId：{}，topK：{}，workspaceId：{}",
            queryText, userId, topK, workspaceId);

        try {
            List<KnowledgeReference> references = vectorSearchService.searchSimilar(queryText, userId, topK, userApiKey);
            return references.stream()
                    .map(ref -> {
                        KnowledgeNode node = knowledgeNodeMapper.selectById(ref.getKnowledgeId());
                        if (node != null && hasAccess(node, userId, workspaceId)) {
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

            LambdaQueryWrapper<KnowledgeNode> wrapper = buildBaseWrapper(userId, workspaceId);
            wrapper.and(w -> w.like(KnowledgeNode::getTitle, queryText)
                    .or()
                    .like(KnowledgeNode::getSummary, queryText));
            wrapper.orderByDesc(KnowledgeNode::getCreateTime);
            wrapper.last("LIMIT " + topK);

            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(wrapper);
            return nodes.stream().map(this::convertToVO).collect(Collectors.toList());
        }
    }

    /**
     * 统计用户的知识节点数量.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 知识节点数量
     */
    @Override
    public long countByUserId(Long userId, Long workspaceId) {
        return knowledgeNodeMapper.selectCount(buildBaseWrapper(userId, workspaceId));
    }

    /**
     * 统计用户在指定时间范围内的知识节点数量.
     *
     * @param userId      用户ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param workspaceId 工作区ID
     * @return 知识节点数量
     */
    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId) {
        return knowledgeNodeMapper.selectCount(
                buildBaseWrapper(userId, workspaceId)
                        .ge(KnowledgeNode::getCreateTime, startTime)
                        .lt(KnowledgeNode::getCreateTime, endTime));
    }

    /**
     * 同步知识节点到Elasticsearch.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void syncToElasticsearch(Long userId, Long workspaceId) {
        if (elasticsearchService == null) {
            log.warn("Elasticsearch服务未启用，跳过同步");
            return;
        }

        try {
            log.info("开始同步知识点到Elasticsearch，userId：{}，workspaceId：{}", userId, workspaceId);

            List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(buildBaseWrapper(userId, workspaceId));

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

    private KnowledgeNodeVO convertToVO(KnowledgeNode node) {
        if (node == null) {
            return null;
        }
        KnowledgeNodeVO vo = new KnowledgeNodeVO();
        BeanUtils.copyProperties(node, vo);
        vo.setTags(knowledgeTagService.listByNode(node.getId()));
        return vo;
    }

    /**
     * 构建基础查询条件：workspaceId 不为 null 时按 workspaceId 过滤，否则按 userId 过滤（向后兼容）.
     */
    private LambdaQueryWrapper<KnowledgeNode> buildBaseWrapper(Long userId, Long workspaceId) {
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        if (workspaceId != null) {
            wrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(KnowledgeNode::getUserId, userId);
        }
        return wrapper;
    }

    /**
     * 校验用户是否有权访问知识点.
     */
    private boolean hasAccess(KnowledgeNode node, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            return workspaceId.equals(node.getWorkspaceId());
        }
        return node.getUserId().equals(userId);
    }
}
