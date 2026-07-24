package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 复习卡片服务实现类.
 * <p>提供复习卡片的生成、提交、调度及统计功能</p>
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

    public ReviewCardServiceImpl(ReviewCardMapper reviewCardMapper, KnowledgeNodeMapper knowledgeNodeMapper,
                             EbbinghausService ebbinghausService, QuestionGenerationService questionGenerationService,
                             ImportanceCalculationService importanceCalculationService, JdbcTemplate jdbcTemplate,
                             GamificationService gamificationService) {
        this.reviewCardMapper = reviewCardMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.ebbinghausService = ebbinghausService;
        this.questionGenerationService = questionGenerationService;
        this.importanceCalculationService = importanceCalculationService;
        this.jdbcTemplate = jdbcTemplate;
        this.gamificationService = gamificationService;
    }

    /**
     * 生成复习卡片（默认自动生成）.
     *
     * @param nodeId   知识节点ID
     * @param cardType 卡片类型
     * @param userId   卡片归属用户ID
     * @return 复习卡片
     */
    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType, Long userId) {
        return generateReviewCard(nodeId, cardType, "auto", userId);
    }

    /**
     * 生成复习卡片（指定生成类型）.
     *
     * @param nodeId         知识节点ID
     * @param cardType       卡片类型
     * @param generationType 生成类型
     * @param userId         卡片归属用户ID
     * @return 复习卡片
     */
    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType, Long userId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", nodeId);
            return null;
        }

        ReviewCard card = questionGenerationService.generateHighQualityQuestion(node, cardType, userId);
        card.setGenerationType(generationType);
        card.setWorkspaceId(node.getWorkspaceId());

        reviewCardMapper.insert(card);
        log.info("生成复习卡片成功，nodeId：{}，cardType：{}，generationType：{}，cardId：{}",
                nodeId, cardType, generationType, card.getId());

        return card;
    }

    /**
     * 获取今日待复习卡片列表.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID（null 时仅按 userId 过滤）
     * @return 待复习卡片列表
     */
    @Override
    public List<ReviewCard> getTodayReviewCards(Long userId, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.eq(ReviewCard::getStatus, 0);
        wrapper.orderByAsc(ReviewCard::getNextReviewTime);

        List<ReviewCard> cards = reviewCardMapper.selectList(wrapper);
        log.info("获取待复习卡片，userId：{}，workspaceId：{}，卡片数：{}", userId, workspaceId, cards.size());
        return cards;
    }

    /**
     * 根据ID获取复习卡片.
     *
     * @param id 卡片ID
     * @return 复习卡片
     */
    @Override
    public ReviewCard getReviewCardById(Long id) {
        return reviewCardMapper.selectById(id);
    }

    /**
     * 提交复习结果.
     *
     * @param cardId     卡片ID
     * @param userAnswer 用户答案
     * @param duration   答题耗时（秒）
     * @param userId     当前用户ID（用于权限校验）
     * @return 复习结果DTO
     */
    @Override
    public ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration, Long userId) {
        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null) {
            log.warn("复习卡片不存在，cardId：{}", cardId);
            return new ReviewResultDTO(false, "", null, "复习卡片不存在");
        }

        if (!card.getUserId().equals(userId)) {
            log.warn("用户无权操作此卡片，cardId：{}，ownerUserId：{}，requestUserId：{}", cardId, card.getUserId(), userId);
            return new ReviewResultDTO(false, "", null, "无权操作此卡片");
        }

        boolean isCorrect = checkAnswer(card, userAnswer);

        card.setReviewCount(card.getReviewCount() + 1);
        
        if (isCorrect) {
            card.setCorrectCount(card.getCorrectCount() + 1);
        } else {
            card.setIncorrectCount(card.getIncorrectCount() + 1);
        }

        // 使用答对率计算掌握程度（不再保存准确率到卡片）
        double accuracy = card.getReviewCount() > 0 ? (double) card.getCorrectCount() / card.getReviewCount() : 0.0;
        int masteryLevel = ebbinghausService.calculateMasteryLevel(card.getReviewCount(), accuracy);
        card.setMasteryLevel(masteryLevel);

        double memoryStrength = ebbinghausService.calculateMemoryStrength(card.getReviewCount(), accuracy);
        card.setMemoryStrength(memoryStrength);

        card.setLastReviewTime(LocalDateTime.now());
        
        card.setStatus(1);

        reviewCardMapper.updateById(card);

        // 游戏化积分奖励 — 失败不影响复习主流程
        gamificationService.awardReviewPoints(userId, card.getDifficulty(), isCorrect, cardId);

        boolean isAutoGenerated = "auto".equals(card.getGenerationType());
        
        if (isAutoGenerated) {
            updateReviewSchedule(cardId, isCorrect);
            syncToKnowledgeNode(card);
            log.info("提交自动生成复习结果成功，cardId：{}，isCorrect：{}，duration：{}秒，reviewCount：{}，masteryLevel：{}",
                    cardId, isCorrect, duration, card.getReviewCount(), masteryLevel);
        } else {
            log.info("提交手动生成练习结果成功，cardId：{}，isCorrect：{}，duration：{}秒",
                    cardId, isCorrect, duration);
        }

        String explanation = extractExplanation(card.getQuestion());
        String message = isCorrect ? "回答正确！继续保持！" : "回答错误，正确答案是：" + card.getAnswer();
        
        ReviewResultDTO result = new ReviewResultDTO(isCorrect, card.getAnswer(), explanation, message);
        
        log.info("复习卡片已完成，cardId：{}", cardId);
        
        return result;
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

    private boolean checkAnswer(ReviewCard card, String userAnswer) {
        String correctAnswer = card.getAnswer();
        if (correctAnswer == null || correctAnswer.isEmpty()) {
            log.warn("卡片没有正确答案，cardId：{}", card.getId());
            return false;
        }

        log.info("检查答案，cardId：{}，userAnswer：'{}'，correctAnswer：'{}'，cardType：'{}'",
                card.getId(), userAnswer, correctAnswer, card.getCardType());

        if ("choice".equals(card.getCardType())) {
            boolean result = correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
            log.info("选择题答案比较结果：{}，trimmedUserAnswer：'{}'，trimmedCorrectAnswer：'{}'",
                    result, userAnswer.trim(), correctAnswer.trim());
            return result;
        } else {
            boolean result = correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
            log.info("其他题型答案比较结果：{}，trimmedUserAnswer：'{}'，trimmedCorrectAnswer：'{}'",
                    result, userAnswer.trim(), correctAnswer.trim());
            return result;
        }
    }

    /**
     * 应用用户+工作区双重过滤条件.
     * 复习卡片始终按 userId 隔离（个人数据），workspaceId 作为额外限定范围.
     * workspaceId 为 null 时仅按 userId 过滤，兼容迁移前未分配工作区的历史数据.
     */
    private void applyUserOrWorkspaceFilter(LambdaQueryWrapper<ReviewCard> wrapper, Long userId, Long workspaceId) {
        wrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null) {
            wrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        }
    }

    private void syncToKnowledgeNode(ReviewCard card) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(card.getNodeId());
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", card.getNodeId());
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

        log.debug("同步复习数据到知识点，nodeId：{}，reviewCount：{}，masteryLevel：{}，importance：{}，nextReviewTime：{}",
                node.getId(), totalReviewCount, overallMasteryLevel, importance, card.getNextReviewTime());
    }

    /**
     * 更新复习计划.
     *
     * @param cardId    卡片ID
     * @param isCorrect 是否答对
     * @return void
     */
    @Override
    public void updateReviewSchedule(Long cardId, boolean isCorrect) {
        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null) {
            log.warn("复习卡片不存在，cardId：{}", cardId);
            return;
        }

        LocalDateTime nextReviewTime = ebbinghausService.calculateNextReviewTime(
                card.getLastReviewTime(),
                card.getReviewCount(),
                isCorrect
        );

        card.setNextReviewTime(nextReviewTime);
        reviewCardMapper.updateById(card);

        log.info("更新复习计划成功，cardId：{}，isCorrect：{}，nextReviewTime：{}",
                cardId, isCorrect, nextReviewTime);
    }

    /**
     * 根据知识点ID获取复习卡片列表（含用户隔离）.
     *
     * @param nodeId      知识节点ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 复习卡片列表
     */
    @Override
    public List<ReviewCard> getReviewCardsByNodeId(Long nodeId, Long userId, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getNodeId, nodeId);
        wrapper.eq(ReviewCard::getDeleted, 0);
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.orderByAsc(ReviewCard::getCreateTime);

        return reviewCardMapper.selectList(wrapper);
    }

    /**
     * 删除复习卡片（软删除）.
     *
     * @param id     卡片ID
     * @param userId 当前用户ID（用于权限校验）
     * @return void
     */
    @Override
    public void deleteReviewCard(Long id, Long userId) {
        ReviewCard card = reviewCardMapper.selectById(id);
        if (card != null) {
            if (!card.getUserId().equals(userId)) {
                log.warn("用户无权删除此卡片，cardId：{}，ownerUserId：{}，requestUserId：{}", id, card.getUserId(), userId);
                throw new com.secondbrain.exception.WorkspaceAccessDeniedException("无权删除此卡片");
            }
            card.setDeleted(1);
            reviewCardMapper.updateById(card);
            log.info("删除复习卡片成功，cardId：{}", id);
        }
    }

    /**
     * 删除所有复习卡片（软删除）.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    @Override
    public void deleteAllReviewCards(Long userId, Long workspaceId) {
        LambdaUpdateWrapper<ReviewCard> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(ReviewCard::getDeleted, 1);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.eq(ReviewCard::getUserId, userId);
        if (workspaceId != null) {
            wrapper.eq(ReviewCard::getWorkspaceId, workspaceId);
        }
        int deletedCount = reviewCardMapper.update(null, wrapper);
        log.info("软删除所有复习卡片成功，userId：{}，共{}张", userId, deletedCount);
    }

    /**
     * 恢复已删除的复习卡片.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 恢复的卡片数量
     */
    @Override
    public int restoreReviewCards(Long userId, Long workspaceId) {
        // 使用 JdbcTemplate 执行原生 SQL，绕过@TableLogic 的自动处理
        // 恢复卡片：将 deleted 设为 0，同时将 is_restored 设为 1
        int count;
        if (workspaceId != null) {
            String sql = "UPDATE review_card SET deleted = 0, is_restored = 1 WHERE user_id = ? AND workspace_id = ? AND deleted = 1";
            count = jdbcTemplate.update(sql, userId, workspaceId);
        } else {
            String sql = "UPDATE review_card SET deleted = 0, is_restored = 1 WHERE user_id = ? AND deleted = 1";
            count = jdbcTemplate.update(sql, userId);
        }

        log.info("恢复复习卡片成功，userId：{}，workspaceId：{}，共{}张", userId, workspaceId, count);
        return count;
    }

    /**
     * 为用户的所有知识点生成复习卡片.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 生成的卡片数量
     */
    @Override
    public int generateReviewCardsForAllNodes(Long userId, Long workspaceId) {
        log.info("开始为用户{}的所有知识点生成手动练习卡片, workspaceId={}", userId, workspaceId);

        LambdaQueryWrapper<KnowledgeNode> nodeWrapper = new LambdaQueryWrapper<KnowledgeNode>()
                .eq(KnowledgeNode::getDeleted, 0);
        if (workspaceId != null) {
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
                        log.info("生成手动练习卡片成功，nodeId：{}，cardType：{}，cardId：{}", 
                                node.getId(), "choice", card.getId());
                    }
                }
            } catch (Exception e) {
                log.error("生成手动练习卡片失败，nodeId：{}", node.getId(), e);
            }
        }

        log.info("为用户{}的所有知识点生成手动练习卡片完成，共生成{}张卡片", userId, generatedCount);
        return generatedCount;
    }

    /**
     * 异步生成所有知识点的复习卡片.
     *
     * @return void
     */
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

    /**
     * 更新缺失正确答案的复习卡片.
     *
     * @return void
     */
    @Override
    public void updateMissingAnswers() {
        log.info("开始更新缺失正确答案的复习卡片");
        
        List<ReviewCard> allCards = reviewCardMapper.selectList(
                new LambdaQueryWrapper<ReviewCard>()
                        .eq(ReviewCard::getDeleted, 0)
                        .isNull(ReviewCard::getAnswer)
        );

        int updatedCount = 0;
        for (ReviewCard card : allCards) {
            try {
                if ("choice".equals(card.getCardType())) {
                    String correctAnswer = extractCorrectAnswerFromQuestion(card.getQuestion());
                    if (correctAnswer != null) {
                        card.setAnswer(correctAnswer);
                        reviewCardMapper.updateById(card);
                        updatedCount++;
                        log.info("更新复习卡片答案成功，cardId：{}，correctAnswer：{}", card.getId(), correctAnswer);
                    }
                }
            } catch (Exception e) {
                log.error("更新复习卡片答案失败，cardId：{}", card.getId(), e);
            }
        }

        log.info("更新缺失正确答案的复习卡片完成，共更新{}张卡片", updatedCount);
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

    /**
     * 统计用户的待复习卡片数量.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 待复习卡片数量
     */
    @Override
    public long countPendingByUserId(Long userId, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.eq(ReviewCard::getReviewCount, 0);
        wrapper.eq(ReviewCard::getDeleted, 0);
        return reviewCardMapper.selectCount(wrapper);
    }

    /**
     * 统计用户已完成的卡片数量.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 已完成的卡片数量
     */
    @Override
    public long countCompletedByUserId(Long userId, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.gt(ReviewCard::getReviewCount, 0);
        wrapper.eq(ReviewCard::getDeleted, 0);
        return reviewCardMapper.selectCount(wrapper);
    }

    /**
     * 统计用户在指定时间范围内的卡片数量.
     *
     * @param userId      用户ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param workspaceId 工作区ID
     * @return 卡片数量
     */
    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.gt(ReviewCard::getReviewCount, 0);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.ge(ReviewCard::getNextReviewTime, startTime);
        wrapper.lt(ReviewCard::getNextReviewTime, endTime);
        return reviewCardMapper.selectCount(wrapper);
    }

    /**
     * 计算用户的连续复习天数.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 连续复习天数
     */
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

            LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
            applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
            wrapper.gt(ReviewCard::getReviewCount, 0);
            wrapper.eq(ReviewCard::getDeleted, 0);
            wrapper.ge(ReviewCard::getLastReviewTime, dayStart);
            wrapper.lt(ReviewCard::getLastReviewTime, dayEnd);
            long count = reviewCardMapper.selectCount(wrapper);

            if (count > 0) {
                streakDays++;
                currentDate = currentDate.minusDays(1);
            } else {
                break;
            }
        }

        return streakDays;
    }

    /**
     * 记录题目质量反馈.
     *
     * @param cardId  卡片ID
     * @param rating  评分
     * @param comment 评论
     * @return void
     */
    @Override
    public void recordQualityFeedback(Long cardId, Integer rating, String comment) {
        try {
            ReviewCard card = reviewCardMapper.selectById(cardId);
            if (card == null) {
                log.warn("记录质量反馈失败，卡片不存在，cardId：{}", cardId);
                return;
            }

            log.info("记录题目质量反馈，cardId：{}，rating：{}，comment：{}", cardId, rating, comment);

            if (rating != null && rating < 3) {
                log.warn("题目质量评价较低，cardId：{}，rating：{}，comment：{}，建议优化题目生成策略", 
                        cardId, rating, comment);
            }

            if (comment != null && !comment.isEmpty()) {
                log.info("用户反馈内容：{}", comment);
            }

        } catch (Exception e) {
            log.error("记录质量反馈失败，cardId：{}", cardId, e);
        }
    }

    /**
     * 获取用户答题准确率.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 准确率（0-100）
     */
    @Override
    public int getUserAccuracy(Long userId, Long workspaceId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        applyUserOrWorkspaceFilter(wrapper, userId, workspaceId);
        wrapper.gt(ReviewCard::getReviewCount, 0);
        wrapper.eq(ReviewCard::getDeleted, 0);
        List<ReviewCard> cards = reviewCardMapper.selectList(wrapper);

        if (cards.isEmpty()) {
            return 0;
        }

        // 计算总答题次数和答对次数
        int totalAttempts = 0;
        int totalCorrect = 0;

        for (ReviewCard card : cards) {
            totalAttempts += card.getReviewCount();
            totalCorrect += card.getCorrectCount();
        }

        // 计算准确率
        if (totalAttempts == 0) {
            return 0;
        }

        int accuracy = (int) Math.round((double) totalCorrect / totalAttempts * 100);
        log.info("用户全局准确率，userId：{}，总答题：{}，答对：{}，准确率：{}%", 
                userId, totalAttempts, totalCorrect, accuracy);
        
        return accuracy;
    }
}
