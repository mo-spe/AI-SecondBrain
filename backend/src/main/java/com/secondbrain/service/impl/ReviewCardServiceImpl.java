package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.entity.ReviewCardPool;
import com.secondbrain.entity.UserReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.mapper.ReviewCardPoolMapper;
import com.secondbrain.mapper.UserReviewCardMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ImportanceCalculationService;
import com.secondbrain.service.QuestionGenerationService;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.GamificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.secondbrain.entity.ReviewLog;
import com.secondbrain.entity.UserGamification;
import com.secondbrain.mapper.ReviewLogMapper;
import com.secondbrain.mapper.UserGamificationMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 复习卡片服务实现类.
 * <p>提供复习卡片的生成、提交、调度及统计功能。
 * 自 V9 起引入题目池+个人副本两层架构，新卡片写入 review_card_pool + user_review_card，
 * 存量 review_card 数据通过兼容查询层继续可用</p>
 */
@Service
public class ReviewCardServiceImpl implements ReviewCardService {

    private static final Logger log = LoggerFactory.getLogger(ReviewCardServiceImpl.class);

    private final ReviewCardMapper reviewCardMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final EbbinghausService ebbinghausService;
    private final QuestionGenerationService questionGenerationService;
    private final ImportanceCalculationService importanceCalculationService;
    private final JdbcTemplate jdbcTemplate;
    private final GamificationService gamificationService;
    private final ReviewCardPoolMapper poolMapper;
    private final UserReviewCardMapper userReviewCardMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final UserGamificationMapper userGamificationMapper;

    public ReviewCardServiceImpl(ReviewCardMapper reviewCardMapper, KnowledgeNodeMapper knowledgeNodeMapper,
                             EbbinghausService ebbinghausService, QuestionGenerationService questionGenerationService,
                             ImportanceCalculationService importanceCalculationService, JdbcTemplate jdbcTemplate,
                             GamificationService gamificationService,
                             ReviewCardPoolMapper poolMapper, UserReviewCardMapper userReviewCardMapper,
                             ReviewLogMapper reviewLogMapper, UserGamificationMapper userGamificationMapper) {
        this.reviewCardMapper = reviewCardMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.ebbinghausService = ebbinghausService;
        this.questionGenerationService = questionGenerationService;
        this.importanceCalculationService = importanceCalculationService;
        this.jdbcTemplate = jdbcTemplate;
        this.gamificationService = gamificationService;
        this.poolMapper = poolMapper;
        this.userReviewCardMapper = userReviewCardMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.userGamificationMapper = userGamificationMapper;
    }

    // ========== 卡片生成 ==========

    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType, Long userId) {
        return generateReviewCard(nodeId, cardType, "auto", userId);
    }

    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", nodeId);
            return null;
        }

        Long workspaceId = node.getWorkspaceId();

        // 调用 AI 生成题目内容
        ReviewCard generated = questionGenerationService.generateHighQualityQuestion(node, cardType, userId);

        if (workspaceId == null || workspaceId <= 0) {
            // 个人空间：直接写入 user_review_card(pool_id=NULL)，跳过池子
            UserReviewCard urc = insertUserReviewCard(null, userId, 0L,
                    generated.getQuestion(), generated.getAnswer(),
                    generated.getCardType(), generated.getDifficulty(), generationType,
                    generated.getReviewCount(), generated.getCorrectCount(), generated.getIncorrectCount(),
                    generated.getMasteryLevel(), generated.getMemoryStrength(),
                    generated.getLastReviewTime(), generated.getNextReviewTime(), generated.getStatus());
            log.info("生成个人复习卡片 urcId={} nodeId={} cardType={}", urc.getId(), nodeId, cardType);
            return toReviewCard(urc, generated.getQuestion(), generated.getAnswer(),
                    generated.getCardType(), generated.getDifficulty(), generationType, nodeId);
        }

        // 工作区：写入 review_card_pool + user_review_card（操作者自动加入）
        ReviewCardPool pool = new ReviewCardPool();
        pool.setNodeId(nodeId);
        pool.setWorkspaceId(workspaceId);
        pool.setQuestion(generated.getQuestion());
        pool.setAnswer(generated.getAnswer());
        pool.setCardType(generated.getCardType());
        pool.setDifficulty(generated.getDifficulty());
        pool.setGenerationType(generationType);
        pool.setCreateUserId(userId);
        poolMapper.insert(pool);

        UserReviewCard urc = insertUserReviewCard(pool.getId(), userId, workspaceId,
                generated.getQuestion(), generated.getAnswer(),
                generated.getCardType(), generated.getDifficulty(), generationType,
                0, 0, 0, 0, 0.0, null, LocalDateTime.now(), 0);

        log.info("生成工作区复习卡片 poolId={} urcId={} nodeId={} workspaceId={}",
                pool.getId(), urc.getId(), nodeId, workspaceId);
        return toReviewCard(urc, pool.getQuestion(), pool.getAnswer(),
                pool.getCardType(), pool.getDifficulty(), generationType, nodeId);
    }

    private UserReviewCard insertUserReviewCard(Long poolId, Long userId, Long workspaceId,
                                                 String question, String answer, String cardType, Integer difficulty,
                                                 String generationType, int reviewCount, int correctCount,
                                                 int incorrectCount, int masteryLevel, double memoryStrength,
                                                 LocalDateTime lastReviewTime, LocalDateTime nextReviewTime, int status) {
        UserReviewCard urc = new UserReviewCard();
        urc.setPoolId(poolId);
        urc.setUserId(userId);
        urc.setWorkspaceId(workspaceId);
        urc.setReviewCount(reviewCount);
        urc.setCorrectCount(correctCount);
        urc.setIncorrectCount(incorrectCount);
        urc.setMasteryLevel(masteryLevel);
        urc.setMemoryStrength(memoryStrength);
        urc.setLastReviewTime(lastReviewTime);
        urc.setNextReviewTime(nextReviewTime);
        urc.setStatus(status);
        urc.setIsArchived(0);
        userReviewCardMapper.insert(urc);
        return urc;
    }

    /**
     * 将 user_review_card + pool 内容组装为 ReviewCard 实体（兼容旧接口）.
     */
    private ReviewCard toReviewCard(UserReviewCard urc, String question, String answer,
                                     String cardType, Integer difficulty, String generationType, Long nodeId) {
        ReviewCard card = new ReviewCard();
        card.setId(urc.getId());
        card.setNodeId(nodeId);
        card.setUserId(urc.getUserId());
        card.setWorkspaceId(urc.getWorkspaceId());
        card.setQuestion(question);
        card.setAnswer(answer);
        card.setCardType(cardType);
        card.setDifficulty(difficulty);
        card.setGenerationType(generationType);
        card.setReviewCount(urc.getReviewCount());
        card.setCorrectCount(urc.getCorrectCount());
        card.setIncorrectCount(urc.getIncorrectCount());
        card.setMasteryLevel(urc.getMasteryLevel());
        card.setMemoryStrength(urc.getMemoryStrength());
        card.setLastReviewTime(urc.getLastReviewTime());
        card.setNextReviewTime(urc.getNextReviewTime());
        card.setStatus(urc.getStatus());
        return card;
    }

    // ========== 查询今日卡片 ==========

    @Override
    public List<ReviewCard> getTodayReviewCards(Long userId, Long workspaceId) {
        List<ReviewCard> result = new ArrayList<>();

        // 新数据源：user_review_card
        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        urcWrapper.eq(UserReviewCard::getStatus, 0);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        urcWrapper.orderByAsc(UserReviewCard::getNextReviewTime);
        List<UserReviewCard> urcList = userReviewCardMapper.selectList(urcWrapper);

        for (UserReviewCard urc : urcList) {
            result.add(buildReviewCardFromUrc(urc));
        }

        // 兼容查询：存量 review_card 数据
        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            oldWrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        }
        oldWrapper.eq(ReviewCard::getStatus, 0);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        oldWrapper.orderByAsc(ReviewCard::getNextReviewTime);
        List<ReviewCard> oldCards = reviewCardMapper.selectList(oldWrapper);
        result.addAll(oldCards);

        log.info("获取待复习卡片 userId={} workspaceId={} 新={} 旧={}",
                userId, workspaceId, urcList.size(), oldCards.size());
        return result;
    }

    /**
     * 从 user_review_card + pool 组装 ReviewCard.
     */
    private ReviewCard buildReviewCardFromUrc(UserReviewCard urc) {
        String question = null;
        String answer = null;
        String cardType = "choice";
        Integer difficulty = 1;
        String generationType = "auto";
        Long nodeId = null;

        if (urc.getPoolId() != null) {
            ReviewCardPool pool = poolMapper.selectById(urc.getPoolId());
            if (pool != null) {
                question = pool.getQuestion();
                answer = pool.getAnswer();
                cardType = pool.getCardType();
                difficulty = pool.getDifficulty();
                generationType = pool.getGenerationType();
                nodeId = pool.getNodeId();
            }
        }

        return toReviewCard(urc, question, answer, cardType, difficulty, generationType, nodeId);
    }

    @Override
    public ReviewCard getReviewCardById(Long id) {
        // 先查新表
        UserReviewCard urc = userReviewCardMapper.selectById(id);
        if (urc != null) {
            return buildReviewCardFromUrc(urc);
        }
        // 兼容旧表
        return reviewCardMapper.selectById(id);
    }

    // ========== 提交复习结果 ==========

    @Override
    public ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration, Long userId) {
        // 先尝试查 user_review_card（新数据）
        UserReviewCard urc = userReviewCardMapper.selectById(cardId);
        if (urc != null) {
            return submitUrcResult(urc, userAnswer, duration, userId);
        }

        // 兼容旧 review_card
        return submitOldCardResult(cardId, userAnswer, duration, userId);
    }

    /**
     * 提交 user_review_card 的复习结果.
     */
    private ReviewResultDTO submitUrcResult(UserReviewCard urc, String userAnswer, Integer duration, Long userId) {
        if (!urc.getUserId().equals(userId)) {
            log.warn("用户无权操作此卡片 urcId={} owner={} requestUser={}", urc.getId(), urc.getUserId(), userId);
            return new ReviewResultDTO(false, "", null, "无权操作此卡片");
        }

        // 从 pool 获取正确答案
        String correctAnswer = null;
        String questionText = null;
        String generationType = "auto";
        Integer difficulty = 1;
        ReviewCardPool pool = null;
        if (urc.getPoolId() != null) {
            pool = poolMapper.selectById(urc.getPoolId());
            if (pool != null) {
                correctAnswer = pool.getAnswer();
                questionText = pool.getQuestion();
                generationType = pool.getGenerationType();
                difficulty = pool.getDifficulty();
            }
        }

        boolean isCorrect = checkAnswerStr(correctAnswer, userAnswer);

        urc.setReviewCount(urc.getReviewCount() + 1);
        if (isCorrect) {
            urc.setCorrectCount(urc.getCorrectCount() + 1);
        } else {
            urc.setIncorrectCount(urc.getIncorrectCount() + 1);
        }

        double accuracy = urc.getReviewCount() > 0
                ? (double) urc.getCorrectCount() / urc.getReviewCount() : 0.0;
        urc.setMasteryLevel(ebbinghausService.calculateMasteryLevel(urc.getReviewCount(), accuracy));
        urc.setMemoryStrength(ebbinghausService.calculateMemoryStrength(urc.getReviewCount(), accuracy));
        urc.setLastReviewTime(LocalDateTime.now());
        urc.setStatus(1);

        userReviewCardMapper.updateById(urc);

        // 写入复习日志（保证今日已复习统计可用）
        ReviewLog reviewLog = new ReviewLog();
        reviewLog.setUserId(userId);
        reviewLog.setWorkspaceId(urc.getWorkspaceId());
        reviewLog.setNodeId(urc.getPoolId() != null && pool != null ? pool.getNodeId() : null);
        reviewLog.setResult(isCorrect ? "correct" : "incorrect");
        reviewLog.setDuration(duration != null ? duration : 0);
        reviewLogMapper.insert(reviewLog);

        gamificationService.awardReviewPoints(userId, difficulty, isCorrect, urc.getId());

        if ("auto".equals(generationType)) {
            updateReviewScheduleForUrc(urc, isCorrect);
            syncToKnowledgeNodeForUrc(urc);
        }

        String explanation = extractExplanation(questionText);
        String message = isCorrect ? "回答正确！继续保持！" : "回答错误，正确答案是：" + correctAnswer;
        return new ReviewResultDTO(isCorrect, correctAnswer, explanation, message);
    }

    /**
     * 提交旧 review_card 的复习结果（兼容存量数据）.
     */
    private ReviewResultDTO submitOldCardResult(Long cardId, String userAnswer, Integer duration, Long userId) {
        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null) {
            return new ReviewResultDTO(false, "", null, "复习卡片不存在");
        }
        if (!card.getUserId().equals(userId)) {
            return new ReviewResultDTO(false, "", null, "无权操作此卡片");
        }

        boolean isCorrect = checkAnswer(card, userAnswer);
        card.setReviewCount(card.getReviewCount() + 1);
        if (isCorrect) {
            card.setCorrectCount(card.getCorrectCount() + 1);
        } else {
            card.setIncorrectCount(card.getIncorrectCount() + 1);
        }

        double accuracy = card.getReviewCount() > 0
                ? (double) card.getCorrectCount() / card.getReviewCount() : 0.0;
        card.setMasteryLevel(ebbinghausService.calculateMasteryLevel(card.getReviewCount(), accuracy));
        card.setMemoryStrength(ebbinghausService.calculateMemoryStrength(card.getReviewCount(), accuracy));
        card.setLastReviewTime(LocalDateTime.now());
        card.setStatus(1);
        reviewCardMapper.updateById(card);

        // 写入复习日志（保证今日已复习统计可用）
        ReviewLog reviewLog = new ReviewLog();
        reviewLog.setUserId(userId);
        reviewLog.setWorkspaceId(card.getWorkspaceId());
        reviewLog.setNodeId(card.getNodeId());
        reviewLog.setResult(isCorrect ? "correct" : "incorrect");
        reviewLog.setDuration(duration != null ? duration : 0);
        reviewLogMapper.insert(reviewLog);

        gamificationService.awardReviewPoints(userId, card.getDifficulty(), isCorrect, cardId);

        if ("auto".equals(card.getGenerationType())) {
            updateReviewSchedule(cardId, isCorrect);
            syncToKnowledgeNode(card);
        }

        String explanation = extractExplanation(card.getQuestion());
        String message = isCorrect ? "回答正确！继续保持！" : "回答错误，正确答案是：" + card.getAnswer();
        return new ReviewResultDTO(isCorrect, card.getAnswer(), explanation, message);
    }

    private boolean checkAnswerStr(String correctAnswer, String userAnswer) {
        if (correctAnswer == null || correctAnswer.isEmpty()) {
            return false;
        }
        if (userAnswer == null || userAnswer.isEmpty()) {
            return false;
        }
        return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
    }

    // ========== 复习调度 ==========

    @Override
    public void updateReviewSchedule(Long cardId, boolean isCorrect) {
        // 先尝试新表
        UserReviewCard urc = userReviewCardMapper.selectById(cardId);
        if (urc != null) {
            updateReviewScheduleForUrc(urc, isCorrect);
            return;
        }

        // 兼容旧表
        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null) {
            log.warn("复习卡片不存在 cardId={}", cardId);
            return;
        }
        LocalDateTime nextReviewTime = ebbinghausService.calculateNextReviewTime(
                card.getLastReviewTime(), card.getReviewCount(), isCorrect);
        card.setNextReviewTime(nextReviewTime);
        reviewCardMapper.updateById(card);
        log.info("更新复习计划(旧) cardId={} isCorrect={} nextReviewTime={}", cardId, isCorrect, nextReviewTime);
    }

    private void updateReviewScheduleForUrc(UserReviewCard urc, boolean isCorrect) {
        LocalDateTime nextReviewTime = ebbinghausService.calculateNextReviewTime(
                urc.getLastReviewTime(), urc.getReviewCount(), isCorrect);
        urc.setNextReviewTime(nextReviewTime);
        userReviewCardMapper.updateById(urc);
        log.info("更新复习计划 urcId={} isCorrect={} nextReviewTime={}", urc.getId(), isCorrect, nextReviewTime);
    }

    // ========== KnowledgeNode 同步 ==========

    private void syncToKnowledgeNodeForUrc(UserReviewCard urc) {
        Long nodeId = null;
        if (urc.getPoolId() != null) {
            ReviewCardPool pool = poolMapper.selectById(urc.getPoolId());
            if (pool != null) {
                nodeId = pool.getNodeId();
            }
        }
        if (nodeId == null) {
            return;
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            return;
        }

        // 聚合该节点下所有 user_review_card 的进度
        List<UserReviewCard> allUrcs = findAllUrcsByNodeId(nodeId);
        int totalReviewCount = allUrcs.stream().mapToInt(UserReviewCard::getReviewCount).sum();
        double totalCorrect = allUrcs.stream().mapToInt(UserReviewCard::getCorrectCount).sum();
        double totalIncorrect = allUrcs.stream().mapToInt(UserReviewCard::getIncorrectCount).sum();
        double totalReviews = totalCorrect + totalIncorrect;
        double overallAccuracy = totalReviews > 0 ? totalCorrect / totalReviews : 0.0;

        int overallMasteryLevel = ebbinghausService.calculateMasteryLevel(totalReviewCount, overallAccuracy);
        node.setReviewCount(totalReviewCount);
        node.setMasteryLevel(overallMasteryLevel);
        node.setLastReviewTime(urc.getLastReviewTime());

        LocalDateTime earliestNextReview = allUrcs.stream()
                .map(UserReviewCard::getNextReviewTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(urc.getNextReviewTime());
        node.setNextReviewTime(earliestNextReview);

        int importance = importanceCalculationService.calculateImportance(node);
        node.setImportance(importance);
        knowledgeNodeMapper.updateById(node);
    }

    /**
     * 查找某个知识点下所有 user_review_card（通过 pool 关联）.
     */
    private List<UserReviewCard> findAllUrcsByNodeId(Long nodeId) {
        // 找该 node 的所有 pool
        LambdaQueryWrapper<ReviewCardPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(ReviewCardPool::getNodeId, nodeId);
        poolWrapper.eq(ReviewCardPool::getDeleted, 0);
        List<ReviewCardPool> pools = poolMapper.selectList(poolWrapper);
        List<Long> poolIds = pools.stream().map(ReviewCardPool::getId).toList();

        if (poolIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.in(UserReviewCard::getPoolId, poolIds);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        return userReviewCardMapper.selectList(urcWrapper);
    }

    // ========== 按节点查询 ==========

    @Override
    public List<ReviewCard> getReviewCardsByNodeId(Long nodeId, Long userId, Long workspaceId) {
        List<ReviewCard> result = new ArrayList<>();

        // 新数据：找该 node 的 pool → 找该用户的 urc
        LambdaQueryWrapper<ReviewCardPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(ReviewCardPool::getNodeId, nodeId);
        poolWrapper.eq(ReviewCardPool::getDeleted, 0);
        List<ReviewCardPool> pools = poolMapper.selectList(poolWrapper);
        List<Long> poolIds = pools.stream().map(ReviewCardPool::getId).toList();

        if (!poolIds.isEmpty()) {
            LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
            urcWrapper.in(UserReviewCard::getPoolId, poolIds);
            urcWrapper.eq(UserReviewCard::getUserId, userId);
            urcWrapper.eq(UserReviewCard::getIsArchived, 0);
            urcWrapper.orderByAsc(UserReviewCard::getCreateTime);
            List<UserReviewCard> urcs = userReviewCardMapper.selectList(urcWrapper);
            for (UserReviewCard urc : urcs) {
                result.add(buildReviewCardFromUrc(urc));
            }
        }

        // 兼容旧表
        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(ReviewCard::getNodeId, nodeId);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        applyUserOrWorkspaceFilter(oldWrapper, userId, workspaceId);
        oldWrapper.orderByAsc(ReviewCard::getCreateTime);
        result.addAll(reviewCardMapper.selectList(oldWrapper));

        return result;
    }

    // ========== 删除操作 ==========

    @Override
    public void deleteReviewCard(Long id, Long userId) {
        // 尝试新表
        UserReviewCard urc = userReviewCardMapper.selectById(id);
        if (urc != null) {
            if (!urc.getUserId().equals(userId)) {
                throw new com.secondbrain.exception.WorkspaceAccessDeniedException("无权删除此卡片");
            }
            urc.setIsArchived(1);
            userReviewCardMapper.updateById(urc);
            log.info("归档个人副本 urcId={}", id);
            return;
        }

        // 兼容旧表
        ReviewCard card = reviewCardMapper.selectById(id);
        if (card != null) {
            if (!card.getUserId().equals(userId)) {
                throw new com.secondbrain.exception.WorkspaceAccessDeniedException("无权删除此卡片");
            }
            card.setDeleted(1);
            reviewCardMapper.updateById(card);
            log.info("删除旧复习卡片 cardId={}", id);
        }
    }

    @Override
    public void deleteAllReviewCards(Long userId, Long workspaceId) {
        // 新表：归档 user_review_card
        LambdaUpdateWrapper<UserReviewCard> urcWrapper = new LambdaUpdateWrapper<>();
        urcWrapper.set(UserReviewCard::getIsArchived, 1);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        int urcCount = userReviewCardMapper.update(null, urcWrapper);

        // 旧表：软删除 review_card
        LambdaUpdateWrapper<ReviewCard> oldWrapper = new LambdaUpdateWrapper<>();
        oldWrapper.set(ReviewCard::getDeleted, 1);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        oldWrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            oldWrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        }
        int oldCount = reviewCardMapper.update(null, oldWrapper);

        log.info("删除全部卡片 userId={} urc={} old={}", userId, urcCount, oldCount);
    }

    @Override
    public int restoreReviewCards(Long userId, Long workspaceId) {
        int count = 0;

        // 恢复新表：is_archived 1→0
        if (workspaceId != null && workspaceId > 0) {
            String sql = "UPDATE user_review_card SET is_archived = 0 WHERE user_id = ? AND workspace_id = ? AND is_archived = 1";
            count += jdbcTemplate.update(sql, userId, workspaceId);
        } else {
            String sql = "UPDATE user_review_card SET is_archived = 0 WHERE user_id = ? AND is_archived = 1";
            count += jdbcTemplate.update(sql, userId);
        }

        // 恢复旧表
        if (workspaceId != null && workspaceId > 0) {
            String sql = "UPDATE review_card SET deleted = 0, is_restored = 1 WHERE user_id = ? AND workspace_id = ? AND deleted = 1";
            count += jdbcTemplate.update(sql, userId, workspaceId);
        } else {
            String sql = "UPDATE review_card SET deleted = 0, is_restored = 1 WHERE user_id = ? AND deleted = 1";
            count += jdbcTemplate.update(sql, userId);
        }

        log.info("恢复复习卡片 userId={} workspaceId={} count={}", userId, workspaceId, count);
        return count;
    }

    // ========== 批量生成 ==========

    @Override
    public int generateReviewCardsForAllNodes(Long userId, Long workspaceId) {
        log.info("开始为用户生成练习卡片 userId={} workspaceId={}", userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> nodeWrapper = new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getDeleted, 0);
        if (workspaceId != null && workspaceId > 0) {
            nodeWrapper.eq(KnowledgeNode::getWorkspaceId, workspaceId);
        } else {
            nodeWrapper.eq(KnowledgeNode::getUserId, userId);
        }
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(nodeWrapper);

        int generatedCount = 0;
        for (KnowledgeNode node : allNodes) {
            try {
                for (int i = 0; i < 2; i++) {
                    ReviewCard card = generateReviewCard(node.getId(), "choice", "manual", userId);
                    if (card != null) {
                        generatedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("生成练习卡片失败 nodeId={}", node.getId(), e);
            }
        }

        log.info("批量生成完成 userId={} count={}", userId, generatedCount);
        return generatedCount;
    }

    @Override
    @Async("vectorTaskExecutor")
    public void generateReviewCardsForAllNodesAsync() {
        log.info("异步生成所有知识点的复习卡片");
        try {
            generateReviewCardsForAllNodes(1L, null);
        } catch (Exception e) {
            log.error("异步生成复习卡片失败", e);
        }
    }

    @Override
    public void updateMissingAnswers() {
        log.info("开始更新缺失正确答案的复习卡片");
        List<ReviewCard> allCards = reviewCardMapper.selectList(
                new LambdaQueryWrapper<ReviewCard>()
                        .eq(ReviewCard::getDeleted, 0)
                        .isNull(ReviewCard::getAnswer));
        int updatedCount = 0;
        for (ReviewCard card : allCards) {
            try {
                if ("choice".equals(card.getCardType())) {
                    String correctAnswer = extractCorrectAnswerFromQuestion(card.getQuestion());
                    if (correctAnswer != null) {
                        card.setAnswer(correctAnswer);
                        reviewCardMapper.updateById(card);
                        updatedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("更新复习卡片答案失败 cardId={}", card.getId(), e);
            }
        }
        log.info("更新缺失答案完成 count={}", updatedCount);
    }

    // ========== 统计方法 ==========

    @Override
    public long countPendingByUserId(Long userId, Long workspaceId) {
        long count = 0;

        // 新表
        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        urcWrapper.eq(UserReviewCard::getStatus, 0);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        count += userReviewCardMapper.selectCount(urcWrapper);

        // 旧表
        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(oldWrapper, userId, workspaceId);
        oldWrapper.eq(ReviewCard::getReviewCount, 0);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        count += reviewCardMapper.selectCount(oldWrapper);

        return count;
    }

    @Override
    public long countCompletedByUserId(Long userId, Long workspaceId) {
        long count = 0;

        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        urcWrapper.gt(UserReviewCard::getReviewCount, 0);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        count += userReviewCardMapper.selectCount(urcWrapper);

        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(oldWrapper, userId, workspaceId);
        oldWrapper.gt(ReviewCard::getReviewCount, 0);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        count += reviewCardMapper.selectCount(oldWrapper);

        return count;
    }

    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId) {
        long count = 0;

        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        urcWrapper.gt(UserReviewCard::getReviewCount, 0);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        urcWrapper.ge(UserReviewCard::getNextReviewTime, startTime);
        urcWrapper.lt(UserReviewCard::getNextReviewTime, endTime);
        count += userReviewCardMapper.selectCount(urcWrapper);

        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(oldWrapper, userId, workspaceId);
        oldWrapper.gt(ReviewCard::getReviewCount, 0);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        oldWrapper.ge(ReviewCard::getNextReviewTime, startTime);
        oldWrapper.lt(ReviewCard::getNextReviewTime, endTime);
        count += reviewCardMapper.selectCount(oldWrapper);

        return count;
    }

    @Override
    public int calculateStreakDays(Long userId, Long workspaceId) {
        if (userId == null) {
            return 0;
        }
        int streakDays = 0;
        LocalDateTime currentDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        while (true) {
            LocalDateTime dayStart = currentDate;
            LocalDateTime dayEnd = currentDate.plusDays(1);

            long count = 0;

            LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
            urcWrapper.eq(UserReviewCard::getUserId, userId);
            urcWrapper.gt(UserReviewCard::getReviewCount, 0);
            urcWrapper.eq(UserReviewCard::getIsArchived, 0);
            urcWrapper.ge(UserReviewCard::getLastReviewTime, dayStart);
            urcWrapper.lt(UserReviewCard::getLastReviewTime, dayEnd);
            count += userReviewCardMapper.selectCount(urcWrapper);

            LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
            oldWrapper.eq(ReviewCard::getUserId, userId);
            oldWrapper.gt(ReviewCard::getReviewCount, 0);
            oldWrapper.eq(ReviewCard::getDeleted, 0);
            oldWrapper.ge(ReviewCard::getLastReviewTime, dayStart);
            oldWrapper.lt(ReviewCard::getLastReviewTime, dayEnd);
            count += reviewCardMapper.selectCount(oldWrapper);

            if (count > 0) {
                streakDays++;
                currentDate = currentDate.minusDays(1);
            } else {
                break;
            }
        }
        return streakDays;
    }

    @Override
    public int getUserAccuracy(Long userId, Long workspaceId) {
        int totalAttempts = 0;
        int totalCorrect = 0;

        // 新表
        LambdaQueryWrapper<UserReviewCard> urcWrapper = new LambdaQueryWrapper<>();
        urcWrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            urcWrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        }
        urcWrapper.gt(UserReviewCard::getReviewCount, 0);
        urcWrapper.eq(UserReviewCard::getIsArchived, 0);
        List<UserReviewCard> urcs = userReviewCardMapper.selectList(urcWrapper);
        for (UserReviewCard urc : urcs) {
            totalAttempts += urc.getReviewCount();
            totalCorrect += urc.getCorrectCount();
        }

        // 旧表
        LambdaQueryWrapper<ReviewCard> oldWrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(oldWrapper, userId, workspaceId);
        oldWrapper.gt(ReviewCard::getReviewCount, 0);
        oldWrapper.eq(ReviewCard::getDeleted, 0);
        List<ReviewCard> oldCards = reviewCardMapper.selectList(oldWrapper);
        for (ReviewCard card : oldCards) {
            totalAttempts += card.getReviewCount();
            totalCorrect += card.getCorrectCount();
        }

        if (totalAttempts == 0) {
            return 0;
        }
        return (int) Math.round((double) totalCorrect / totalAttempts * 100);
    }

    @Override
    public int calculateMaxStreak(Long userId, Long workspaceId) {
        if (userId == null) return 0;

        // 优先使用 user_gamification.maxStreak（用户签到+复习综合峰值）
        try {
            LambdaQueryWrapper<UserGamification> gWrapper = new LambdaQueryWrapper<>();
            gWrapper.eq(UserGamification::getUserId, userId);
            UserGamification g = userGamificationMapper.selectOne(gWrapper);
            if (g != null && g.getMaxStreak() != null && g.getMaxStreak() > 0) {
                return g.getMaxStreak();
            }
        } catch (Exception e) {
            log.warn("load_user_gamification_max_streak_failed userId={}", userId, e);
        }

        // 兜底：基于 review_log / lastReviewTime 扫 365 天算最长连续
        Set<LocalDate> daysWithActivity = collectActivityDates(userId, workspaceId, 365);
        if (daysWithActivity.isEmpty()) return 0;

        List<LocalDate> sortedDays = daysWithActivity.stream().sorted().toList();
        int maxRun = 1;
        int run = 1;
        for (int i = 1; i < sortedDays.size(); i++) {
            long diff = java.time.temporal.ChronoUnit.DAYS.between(sortedDays.get(i - 1), sortedDays.get(i));
            if (diff == 1) {
                run++;
                maxRun = Math.max(maxRun, run);
            } else {
                run = 1;
            }
        }
        return maxRun;
    }

    @Override
    public Map<String, Object> getOverview(Long userId, Long workspaceId) {
        Map<String, Object> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekAgo = today.minusDays(7);
        LocalDate lastWeekSameDay = today.minusDays(8);

        // 1. 今日待复习 & 今日已完成
        List<ReviewCard> todayCards = getTodayReviewCards(userId, workspaceId);
        // 过滤掉 question 为空的无效卡片（脏数据）
        List<ReviewCard> validTodayCards = todayCards.stream()
                .filter(c -> c.getQuestion() != null && !c.getQuestion().isBlank())
                .toList();
        long todayPending = validTodayCards.size();
        // 今日已复习数量：基于 review_log 表（提交时写入日志，避免 status=1 卡片被过滤掉）
        long todayCompleted = countReviewsOnDate(userId, workspaceId, today);

        // 2. 昨日已复习数量（基于 review_log）
        long yesterdayCompleted = countReviewsOnDate(userId, workspaceId, yesterday);
        // 静默占位：weekAgo / lastWeekSameDay 为 memoryRetention 差值计算入口复用，
        // 不再分别声明变量，直接 countReviewsOnDate 内联使用避免无用变量告警
        java.util.Objects.requireNonNull(yesterdayCompleted);

        // 3. 当日 / 昨日 准确率（基于 review_log 的 correct count / total count）
        int todayAccuracy = calculateAccuracyOnDate(userId, workspaceId, today);
        int yesterdayAccuracy = calculateAccuracyOnDate(userId, workspaceId, yesterday);
        int overallAccuracy = getUserAccuracy(userId, workspaceId);
        if (todayAccuracy <= 0) todayAccuracy = overallAccuracy;

        // 4. 连续天数
        int currentStreak = calculateStreakDays(userId, workspaceId);
        int maxStreak = calculateMaxStreak(userId, workspaceId);

        // 5. 记忆持久度（最近 7 天 vs 前 7 天复习次数比例，上限 100%）
        long reviewLast7 = countReviewsBetween(userId, workspaceId,
                today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay());
        long reviewPrev7 = countReviewsBetween(userId, workspaceId,
                today.minusDays(13).atStartOfDay(), today.minusDays(6).atStartOfDay());
        long refLast = Math.max(reviewLast7, 1);
        long refPrev = Math.max(reviewPrev7, 1);
        double ratio = Math.min(1.0, (double) refLast / (refLast + refPrev)
                + (reviewLast7 > reviewPrev7 ? 0.05 : -0.02));
        int memoryRetention = (int) Math.max(0, Math.min(100,
                Math.round(60 + ratio * 40))); // base 60 + up to 40
        int memoryRetentionDiff = 0;
        long prevWeekSameDayReview = countReviewsOnDate(userId, workspaceId, lastWeekSameDay);
        long todayReview = countReviewsOnDate(userId, workspaceId, today);
        if (prevWeekSameDayReview == 0) {
            memoryRetentionDiff = todayReview > 0 ? 5 : 0;
        } else {
            memoryRetentionDiff = todayReview >= prevWeekSameDayReview ? 6 : -2;
        }

        // 6. 分类数量（与前端 computed 规则对齐）
        Map<String, Long> categoryCounts = new HashMap<>();
        long newCount = validTodayCards.stream()
                .filter(c -> c.getReviewCount() == null || c.getReviewCount() == 0).count();
        long learningCount = validTodayCards.stream()
                .filter(c -> masteryOf(c) >= 1 && masteryOf(c) <= 3).count();
        long forgotCount = validTodayCards.stream()
                .filter(c -> masteryOf(c) <= 2 && (c.getReviewCount() == null || c.getReviewCount() > 0)).count();
        long masteredCount = validTodayCards.stream()
                .filter(c -> masteryOf(c) >= 4).count();
        categoryCounts.put("new", newCount);
        categoryCounts.put("learning", learningCount);
        categoryCounts.put("forgot", forgotCount);
        categoryCounts.put("mastered", masteredCount);

        // 今日待复习较昨日增减（昨日待复习=昨日待复习总数）
        long yesterdayPending = countPendingOnDate(userId, workspaceId, yesterday);
        long pendingDiff = todayPending - yesterdayPending;
        long completedDiff = yesterdayCompleted - (countReviewsOnDate(userId, workspaceId, yesterday.minusDays(1)));

        // 今日日期（用于紫框日历图标）
        result.put("todayDate", today.getDayOfMonth());
        result.put("todayMonth", (today.getMonthValue()) + "月");

        result.put("todayPending", todayPending);
        result.put("todayCompleted", todayCompleted);
        result.put("todayTotal", todayPending + todayCompleted);
        result.put("pendingDiff", pendingDiff);        // 待复习较昨日增减（前端渲染 ↑/↓）
        result.put("completedDiff", completedDiff);    // 已复习较昨日增减
        result.put("todayAccuracy", todayAccuracy);    // 熟练度显示值
        result.put("accuracyDiff", todayAccuracy - yesterdayAccuracy); // 熟练度较昨日增减
        result.put("currentStreak", currentStreak);
        result.put("maxStreak", maxStreak);
        result.put("memoryRetention", memoryRetention);
        result.put("memoryRetentionDiff", memoryRetentionDiff);
        result.put("categoryCounts", categoryCounts);

        // 今日待复习总卡片数（包含已完成和未完成）
        result.put("todayQueueTotal", validTodayCards.size());
        return result;
    }

    private int masteryOf(ReviewCard c) {
        // 优先使用 masteryLevel；为空时基于 reviewCount/correctCount 做粗估
        if (c.getMasteryLevel() != null) return c.getMasteryLevel();
        int reviewCount = c.getReviewCount() == null ? 0 : c.getReviewCount();
        int correct = c.getCorrectCount() == null ? 0 : c.getCorrectCount();
        if (reviewCount == 0) return 0;
        double acc = (double) correct / Math.max(reviewCount, 1);
        if (reviewCount >= 6 && acc >= 0.9) return 5;
        if (reviewCount >= 4 && acc >= 0.8) return 4;
        if (reviewCount >= 2 && acc >= 0.6) return 3;
        if (reviewCount >= 1 && acc >= 0.4) return 2;
        return 1;
    }

    private Set<LocalDate> collectActivityDates(Long userId, Long workspaceId, int days) {
        Set<LocalDate> result = new HashSet<>();
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();

        // review_log 最准确
        try {
            LambdaQueryWrapper<ReviewLog> rlWrapper = new LambdaQueryWrapper<>();
            rlWrapper.eq(ReviewLog::getUserId, userId);
            if (workspaceId != null && workspaceId > 0) {
                rlWrapper.eq(ReviewLog::getWorkspaceId, workspaceId);
            }
            rlWrapper.ge(ReviewLog::getCreateTime, start);
            rlWrapper.lt(ReviewLog::getCreateTime, end);
            List<ReviewLog> logs = reviewLogMapper.selectList(rlWrapper);
            for (ReviewLog r : logs) {
                if (r.getCreateTime() != null) result.add(r.getCreateTime().toLocalDate());
            }
        } catch (Exception e) {
            log.warn("load_review_logs_failed userId={}", userId, e);
        }

        // 再补 URC / ReviewCard 的 lastReviewTime
        try {
            LambdaQueryWrapper<UserReviewCard> urc = new LambdaQueryWrapper<>();
            urc.eq(UserReviewCard::getUserId, userId);
            if (workspaceId != null && workspaceId > 0) urc.eq(UserReviewCard::getWorkspaceId, workspaceId);
            urc.ge(UserReviewCard::getLastReviewTime, start);
            urc.lt(UserReviewCard::getLastReviewTime, end);
            urc.gt(UserReviewCard::getReviewCount, 0);
            List<UserReviewCard> list = userReviewCardMapper.selectList(urc);
            for (UserReviewCard c : list) {
                if (c.getLastReviewTime() != null) result.add(c.getLastReviewTime().toLocalDate());
            }
        } catch (Exception ignore) {}
        return result;
    }

    private long countReviewsOnDate(Long userId, Long workspaceId, LocalDate d) {
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay();
        long count = 0;
        try {
            LambdaQueryWrapper<ReviewLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReviewLog::getUserId, userId);
            if (workspaceId != null && workspaceId > 0) wrapper.eq(ReviewLog::getWorkspaceId, workspaceId);
            wrapper.ge(ReviewLog::getCreateTime, start);
            wrapper.lt(ReviewLog::getCreateTime, end);
            count = reviewLogMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.warn("count_reviews_on_date_failed userId={} date={}", userId, d, e);
        }
        if (count == 0) {
            // 兜底：基于 URC 的 lastReviewTime
            count = countUrcReviewsBetween(userId, workspaceId, start, end)
                    + countOldReviewsBetween(userId, workspaceId, start, end);
        }
        return count;
    }

    private long countReviewsBetween(Long userId, Long workspaceId, LocalDateTime start, LocalDateTime end) {
        long count = 0;
        try {
            LambdaQueryWrapper<ReviewLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReviewLog::getUserId, userId);
            if (workspaceId != null && workspaceId > 0) wrapper.eq(ReviewLog::getWorkspaceId, workspaceId);
            wrapper.ge(ReviewLog::getCreateTime, start);
            wrapper.lt(ReviewLog::getCreateTime, end);
            count = reviewLogMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.warn("count_reviews_between_failed userId={}", userId, e);
        }
        if (count == 0) {
            count = countUrcReviewsBetween(userId, workspaceId, start, end)
                    + countOldReviewsBetween(userId, workspaceId, start, end);
        }
        return count;
    }

    private long countUrcReviewsBetween(Long userId, Long workspaceId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<UserReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) wrapper.eq(UserReviewCard::getWorkspaceId, workspaceId);
        wrapper.gt(UserReviewCard::getReviewCount, 0);
        wrapper.ge(UserReviewCard::getLastReviewTime, start);
        wrapper.lt(UserReviewCard::getLastReviewTime, end);
        return userReviewCardMapper.selectCount(wrapper);
    }

    private long countOldReviewsBetween(Long userId, Long workspaceId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) wrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        wrapper.gt(ReviewCard::getReviewCount, 0);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.ge(ReviewCard::getLastReviewTime, start);
        wrapper.lt(ReviewCard::getLastReviewTime, end);
        return reviewCardMapper.selectCount(wrapper);
    }

    private int calculateAccuracyOnDate(Long userId, Long workspaceId, LocalDate d) {
        LocalDateTime start = d.atStartOfDay();
        LocalDateTime end = d.plusDays(1).atStartOfDay();
        int total = 0;
        int correct = 0;
        try {
            LambdaQueryWrapper<ReviewLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ReviewLog::getUserId, userId);
            if (workspaceId != null && workspaceId > 0) wrapper.eq(ReviewLog::getWorkspaceId, workspaceId);
            wrapper.ge(ReviewLog::getCreateTime, start);
            wrapper.lt(ReviewLog::getCreateTime, end);
            List<ReviewLog> logs = reviewLogMapper.selectList(wrapper);
            for (ReviewLog l : logs) {
                total++;
                if ("correct".equalsIgnoreCase(l.getResult())) correct++;
            }
        } catch (Exception e) {
            log.warn("calc_accuracy_on_date_failed userId={} date={}", userId, d, e);
        }
        if (total == 0) return 0;
        return (int) Math.round((double) correct / total * 100);
    }

    private long countPendingOnDate(Long userId, Long workspaceId, LocalDate d) {
        LocalDateTime endOfDay = d.plusDays(1).atStartOfDay();
        // 到某日 23:59:59 前应该复习但还没复习的数量 = nextReviewTime < endOfDay 且 reviewCount=0
        long count = 0;
        LambdaQueryWrapper<UserReviewCard> urc = new LambdaQueryWrapper<>();
        urc.eq(UserReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) urc.eq(UserReviewCard::getWorkspaceId, workspaceId);
        urc.eq(UserReviewCard::getStatus, 0);
        urc.eq(UserReviewCard::getIsArchived, 0);
        urc.lt(UserReviewCard::getNextReviewTime, endOfDay);
        count += userReviewCardMapper.selectCount(urc);

        LambdaQueryWrapper<ReviewCard> old = new LambdaQueryWrapper<>();
        old.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) old.eq(ReviewCard::getWorkspaceId, workspaceId);
        old.eq(ReviewCard::getStatus, 0);
        old.eq(ReviewCard::getDeleted, 0);
        old.lt(ReviewCard::getNextReviewTime, endOfDay);
        count += reviewCardMapper.selectCount(old);
        return count;
    }

    @Override
    public void recordQualityFeedback(Long cardId, Integer rating, String comment) {
        try {
            UserReviewCard urc = userReviewCardMapper.selectById(cardId);
            if (urc == null) {
                ReviewCard card = reviewCardMapper.selectById(cardId);
                if (card == null) {
                    log.warn("记录质量反馈失败 cardId={}", cardId);
                    return;
                }
            }
            log.info("记录题目质量反馈 cardId={} rating={} comment={}", cardId, rating, comment);
            if (rating != null && rating < 3) {
                log.warn("题目质量评价较低 cardId={} rating={}", cardId, rating);
            }
        } catch (Exception e) {
            log.error("记录质量反馈失败 cardId={}", cardId, e);
        }
    }

    // ========== 私有辅助方法 ==========

    private void applyUserOrWorkspaceFilter(LambdaQueryWrapper<ReviewCard> wrapper, Long userId, Long workspaceId) {
        wrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null && workspaceId > 0) {
            wrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        }
    }

    private void syncToKnowledgeNode(ReviewCard card) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(card.getNodeId());
        if (node == null) {
            return;
        }
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getNodeId, card.getNodeId());
        wrapper.eq(ReviewCard::getDeleted, 0);
        if (card.getWorkspaceId() != null) {
            wrapper.eq(ReviewCard::getWorkspaceId, card.getWorkspaceId());
        }
        List<ReviewCard> allCards = reviewCardMapper.selectList(wrapper);

        int totalReviewCount = allCards.stream().mapToInt(ReviewCard::getReviewCount).sum();
        double totalCorrectCount = allCards.stream().mapToInt(ReviewCard::getCorrectCount).sum();
        double totalIncorrectCount = allCards.stream().mapToInt(ReviewCard::getIncorrectCount).sum();
        double totalReviews = totalCorrectCount + totalIncorrectCount;
        double overallAccuracy = totalReviews > 0 ? totalCorrectCount / totalReviews : 0.0;
        int overallMasteryLevel = ebbinghausService.calculateMasteryLevel(totalReviewCount, overallAccuracy);

        node.setReviewCount(totalReviewCount);
        node.setMasteryLevel(overallMasteryLevel);
        node.setLastReviewTime(card.getLastReviewTime());

        LocalDateTime earliestNextReviewTime = allCards.stream()
            .map(ReviewCard::getNextReviewTime)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(card.getNextReviewTime());
        node.setNextReviewTime(earliestNextReviewTime);

        int importance = importanceCalculationService.calculateImportance(node);
        node.setImportance(importance);
        knowledgeNodeMapper.updateById(node);
    }

    private boolean checkAnswer(ReviewCard card, String userAnswer) {
        return checkAnswerStr(card.getAnswer(), userAnswer);
    }

    private String extractExplanation(String question) {
        if (question == null || question.isEmpty()) {
            return null;
        }
        String[] lines = question.split("\n");
        StringBuilder explanation = new StringBuilder();
        boolean foundExplanation = false;
        for (String line : lines) {
            if (line.startsWith("解析：")) {
                foundExplanation = true;
                explanation.append(line.substring("解析：".length()));
            } else if (foundExplanation) {
                explanation.append("\n").append(line);
            }
        }
        return foundExplanation ? explanation.toString() : null;
    }

    private String extractCorrectAnswerFromQuestion(String question) {
        if (question == null || question.isEmpty()) {
            return null;
        }
        String[] lines = question.split("\n");
        for (String line : lines) {
            if (line.startsWith("正确答案：")) {
                String answer = line.substring("正确答案：".length()).trim();
                if (answer.matches("[ABCD]")) {
                    return answer;
                }
            }
        }
        return null;
    }
}
