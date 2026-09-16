package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.BatchConfirmRequest;
import com.secondbrain.dto.PendingKnowledgeItem;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.PendingKnowledge;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.PendingKnowledgeMapper;
import com.secondbrain.service.PendingKnowledgeService;
import com.secondbrain.service.ReviewCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待确认知识点服务实现类.
 * <p>AI 提取的知识点先存入 pending_knowledge 表，经用户确认后才迁移到 knowledge_node</p>
 */
@Service
public class PendingKnowledgeServiceImpl implements PendingKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(PendingKnowledgeServiceImpl.class);

    private final PendingKnowledgeMapper pendingKnowledgeMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ReviewCardService reviewCardService;

    public PendingKnowledgeServiceImpl(PendingKnowledgeMapper pendingKnowledgeMapper,
                                        KnowledgeNodeMapper knowledgeNodeMapper,
                                        ReviewCardService reviewCardService) {
        this.pendingKnowledgeMapper = pendingKnowledgeMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.reviewCardService = reviewCardService;
    }

    @Override
    public List<PendingKnowledge> listPending(Long userId, Long workspaceId) {
        LambdaQueryWrapper<PendingKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PendingKnowledge::getUserId, userId);
        if (workspaceId != null) {
            wrapper.eq(PendingKnowledge::getWorkspaceId, workspaceId);
        }
        wrapper.eq(PendingKnowledge::getStatus, 0);
        wrapper.eq(PendingKnowledge::getDeleted, 0);
        wrapper.orderByDesc(PendingKnowledge::getCreateTime);
        return pendingKnowledgeMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void confirmBatch(Long userId, BatchConfirmRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("confirmBatch 请求的 items 为空，userId={}", userId);
            return;
        }

        boolean generateCards = Boolean.TRUE.equals(request.getGenerateCards());
        log.info("confirmBatch userId={} items={} generateCards={}", userId, request.getItems().size(), generateCards);

        for (PendingKnowledgeItem item : request.getItems()) {
            PendingKnowledge pending = pendingKnowledgeMapper.selectById(item.getPendingId());
            if (pending == null || !pending.getUserId().equals(userId)) {
                log.warn("待确认记录不存在或不属于当前用户，pendingId={}", item.getPendingId());
                continue;
            }

            String title = item.getTitle() != null ? item.getTitle() : pending.getTitle();
            String summary = item.getSummary() != null ? item.getSummary() : pending.getSummary();
            String content = item.getContent() != null ? item.getContent() : pending.getContent();

            // 判断是否需要复习：请求参数 > pending 原始标记 > 默认 false
            boolean needReview = generateCards || Boolean.TRUE.equals(pending.getNeedReview());

            KnowledgeNode node = new KnowledgeNode();
            node.setUserId(userId);
            node.setWorkspaceId(pending.getWorkspaceId());
            node.setTitle(title != null ? title : "未命名知识点");
            node.setSummary(summary);
            node.setContentMd(content);
            node.setImportance(3);
            node.setMasteryLevel(0);
            node.setNeedReview(needReview ? 1 : 0);
            node.setReviewCount(0);
            knowledgeNodeMapper.insert(node);
            log.info("知识点入库 nodeId={} title={} needReview={}", node.getId(), node.getTitle(), needReview);

            // 需要复习时为知识点生成复习卡片
            if (needReview) {
                try {
                    reviewCardService.generateReviewCard(node.getId(), "choice", "auto", userId);
                    log.info("生成复习卡片 nodeId={}", node.getId());
                } catch (Exception e) {
                    log.error("生成复习卡片失败 nodeId={}", node.getId(), e);
                }
            }

            pending.setStatus(1);
            pending.setUpdateTime(LocalDateTime.now());
            pendingKnowledgeMapper.updateById(pending);
        }
    }

    @Override
    public void discardPending(Long userId, Long pendingId) {
        PendingKnowledge pending = pendingKnowledgeMapper.selectById(pendingId);
        if (pending == null || !pending.getUserId().equals(userId)) {
            log.warn("待确认记录不存在或不属于当前用户，pendingId={}", pendingId);
            return;
        }
        pending.setStatus(2);
        pending.setUpdateTime(LocalDateTime.now());
        pendingKnowledgeMapper.updateById(pending);
        log.info("丢弃待确认知识点 pendingId={}", pendingId);
    }

    @Override
    public PendingKnowledge addPending(Long userId, PendingKnowledge item) {
        item.setId(null);
        item.setUserId(userId);
        item.setStatus(0);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        pendingKnowledgeMapper.insert(item);
        log.info("手动新增待确认知识点 pendingId={} title={}", item.getId(), item.getTitle());
        return item;
    }
}
