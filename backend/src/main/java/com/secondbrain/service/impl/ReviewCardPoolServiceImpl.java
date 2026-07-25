package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.CommunityLabelDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCardPool;
import com.secondbrain.entity.UserReviewCard;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardPoolMapper;
import com.secondbrain.mapper.UserReviewCardMapper;
import com.secondbrain.service.ReviewCardPoolService;
import com.secondbrain.vo.ReviewCardPoolVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 题目池服务实现类.
 * <p>管理工作区级题目池的 CRUD、社区标签计算（5分钟缓存）和加入复习流程</p>
 */
@Service
public class ReviewCardPoolServiceImpl implements ReviewCardPoolService {

    private static final Logger log = LoggerFactory.getLogger(ReviewCardPoolServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long CACHE_TTL_MS = 300_000; // 5 分钟

    private final ReviewCardPoolMapper poolMapper;
    private final UserReviewCardMapper userReviewCardMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    /**
     * 社区标签缓存，key=poolId, value={data, timestamp}
     */
    private final ConcurrentHashMap<Long, CacheEntry> communityCache = new ConcurrentHashMap<>();

    public ReviewCardPoolServiceImpl(ReviewCardPoolMapper poolMapper,
                                     UserReviewCardMapper userReviewCardMapper,
                                     KnowledgeNodeMapper knowledgeNodeMapper) {
        this.poolMapper = poolMapper;
        this.userReviewCardMapper = userReviewCardMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    @Override
    public List<ReviewCardPoolVO> getPoolList(Long workspaceId, Long userId) {
        LambdaQueryWrapper<ReviewCardPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCardPool::getWorkspaceId, workspaceId);
        wrapper.eq(ReviewCardPool::getDeleted, 0);
        wrapper.orderByDesc(ReviewCardPool::getCreateTime);
        List<ReviewCardPool> pools = poolMapper.selectList(wrapper);

        if (pools.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> poolIds = pools.stream().map(ReviewCardPool::getId).toList();
        Map<Long, CommunityLabelDTO> statsMap = batchCommunityStatsWithCache(poolIds);

        Set<Long> joinedPoolIds = getJoinedPoolIds(userId, poolIds);

        Map<Long, KnowledgeNode> nodeMap = loadNodeMap(pools);

        return pools.stream().map(pool -> buildVO(pool, statsMap.get(pool.getId()), joinedPoolIds, nodeMap)).toList();
    }

    @Override
    public ReviewCardPoolVO getPoolDetail(Long poolId, Long userId) {
        ReviewCardPool pool = poolMapper.selectById(poolId);
        if (pool == null || pool.getDeleted() == 1) {
            throw new BusinessException(404, "题目不存在");
        }

        List<Long> poolIds = Collections.singletonList(poolId);
        Map<Long, CommunityLabelDTO> statsMap = batchCommunityStatsWithCache(poolIds);
        Set<Long> joinedPoolIds = getJoinedPoolIds(userId, poolIds);
        Map<Long, KnowledgeNode> nodeMap = loadNodeMap(Collections.singletonList(pool));

        return buildVO(pool, statsMap.get(poolId), joinedPoolIds, nodeMap);
    }

    @Override
    public UserReviewCard joinPool(Long poolId, Long userId) {
        ReviewCardPool pool = poolMapper.selectById(poolId);
        if (pool == null || pool.getDeleted() == 1) {
            throw new BusinessException(404, "题目不存在");
        }

        // 查是否有非归档副本 → 归档旧副本（重新加入场景）
        LambdaQueryWrapper<UserReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReviewCard::getPoolId, poolId);
        wrapper.eq(UserReviewCard::getUserId, userId);
        wrapper.eq(UserReviewCard::getIsArchived, 0);
        UserReviewCard existing = userReviewCardMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setIsArchived(1);
            userReviewCardMapper.updateById(existing);
            log.info("归档旧副本 urcId={} poolId={} userId={}", existing.getId(), poolId, userId);
        }

        UserReviewCard urc = new UserReviewCard();
        urc.setPoolId(poolId);
        urc.setUserId(userId);
        urc.setWorkspaceId(pool.getWorkspaceId());
        urc.setReviewCount(0);
        urc.setCorrectCount(0);
        urc.setIncorrectCount(0);
        urc.setMasteryLevel(0);
        urc.setMemoryStrength(0.0);
        urc.setStatus(0);
        urc.setIsArchived(0);
        urc.setNextReviewTime(LocalDateTime.now()); // 立即可复习
        userReviewCardMapper.insert(urc);

        // 清除缓存，下次查询时刷新社区标签
        communityCache.remove(poolId);

        log.info("加入复习成功 urcId={} poolId={} userId={}", urc.getId(), poolId, userId);
        return urc;
    }

    @Override
    public void deletePoolItem(Long poolId, Long userId) {
        ReviewCardPool pool = poolMapper.selectById(poolId);
        if (pool == null || pool.getDeleted() == 1) {
            throw new BusinessException(404, "题目不存在");
        }
        if (!pool.getCreateUserId().equals(userId)) {
            throw new BusinessException(403, "只有题目创建者可以删除");
        }

        pool.setDeleted(1);
        poolMapper.updateById(pool);
        communityCache.remove(poolId);
        log.info("删除池子题目 poolId={} userId={}", poolId, userId);
    }

    @Override
    public ReviewCardPool updatePoolItem(Long poolId, String question, String answer, Long userId) {
        ReviewCardPool pool = poolMapper.selectById(poolId);
        if (pool == null || pool.getDeleted() == 1) {
            throw new BusinessException(404, "题目不存在");
        }
        if (!pool.getCreateUserId().equals(userId)) {
            throw new BusinessException(403, "只有题目创建者可以编辑");
        }

        pool.setQuestion(question);
        pool.setAnswer(answer);
        poolMapper.updateById(pool);
        log.info("编辑池子题目 poolId={} userId={}", poolId, userId);
        return pool;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 批量获取社区统计数据（带 5 分钟缓存）.
     */
    private Map<Long, CommunityLabelDTO> batchCommunityStatsWithCache(List<Long> poolIds) {
        Map<Long, CommunityLabelDTO> result = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Long poolId : poolIds) {
            CacheEntry entry = communityCache.get(poolId);
            if (entry != null && (now - entry.timestamp) < CACHE_TTL_MS) {
                result.put(poolId, entry.data);
            } else {
                missIds.add(poolId);
            }
        }

        if (!missIds.isEmpty()) {
            List<CommunityLabelDTO> fresh = userReviewCardMapper.batchGetCommunityStats(missIds);
            Set<Long> hitIds = new HashSet<>();
            for (CommunityLabelDTO dto : fresh) {
                result.put(dto.getPoolId(), dto);
                communityCache.put(dto.getPoolId(), new CacheEntry(dto, now));
                hitIds.add(dto.getPoolId());
            }
            // 池子暂无成员时补空统计
            for (Long id : missIds) {
                if (!hitIds.contains(id)) {
                    CommunityLabelDTO empty = new CommunityLabelDTO();
                    empty.setPoolId(id);
                    empty.setMemberCount(0);
                    empty.setMasteredCount(0);
                    result.put(id, empty);
                    communityCache.put(id, new CacheEntry(empty, now));
                }
            }
        }

        return result;
    }

    /**
     * 获取当前用户已加入的池子ID集合.
     */
    private Set<Long> getJoinedPoolIds(Long userId, List<Long> poolIds) {
        if (poolIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<UserReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReviewCard::getUserId, userId);
        wrapper.in(UserReviewCard::getPoolId, poolIds);
        wrapper.eq(UserReviewCard::getIsArchived, 0);
        List<UserReviewCard> userCards = userReviewCardMapper.selectList(wrapper);
        return userCards.stream().map(UserReviewCard::getPoolId).collect(Collectors.toSet());
    }

    /**
     * 批量加载知识点信息.
     */
    private Map<Long, KnowledgeNode> loadNodeMap(List<ReviewCardPool> pools) {
        Set<Long> nodeIds = pools.stream().map(ReviewCardPool::getNodeId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectBatchIds(nodeIds);
        return nodes.stream().collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a));
    }

    /**
     * 组装 ReviewCardPoolVO.
     */
    private ReviewCardPoolVO buildVO(ReviewCardPool pool, CommunityLabelDTO stats,
                                     Set<Long> joinedPoolIds, Map<Long, KnowledgeNode> nodeMap) {
        ReviewCardPoolVO vo = new ReviewCardPoolVO();
        vo.setId(pool.getId());
        vo.setNodeId(pool.getNodeId());
        vo.setCardType(pool.getCardType());
        vo.setDifficulty(pool.getDifficulty());
        vo.setGenerationType(pool.getGenerationType());
        vo.setCreateUserId(pool.getCreateUserId());

        if (pool.getQuestion() != null) {
            int len = Math.min(pool.getQuestion().length(), 100);
            vo.setQuestionPreview(pool.getQuestion().substring(0, len));
        }

        if (pool.getCreateTime() != null) {
            vo.setCreateTime(pool.getCreateTime().format(FORMATTER));
        }

        KnowledgeNode node = nodeMap.get(pool.getNodeId());
        if (node != null) {
            vo.setNodeTitle(node.getTitle());
        }

        if (stats != null) {
            vo.setMemberCount(stats.getMemberCount());
            vo.setCommunityLabel(stats.getLabel());
            vo.setCommunityText(stats.getLabelText());
        }

        boolean joined = joinedPoolIds.contains(pool.getId());
        vo.setIsJoined(joined);

        return vo;
    }

    /**
     * 缓存条目.
     */
    private static class CacheEntry {
        final CommunityLabelDTO data;
        final long timestamp;

        CacheEntry(CommunityLabelDTO data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
