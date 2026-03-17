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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ReviewCardServiceImpl implements ReviewCardService {

    private static final Logger log = LoggerFactory.getLogger(ReviewCardServiceImpl.class);

    private final ReviewCardMapper reviewCardMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final EbbinghausService ebbinghausService;
    private final QuestionGenerationService questionGenerationService;
    private final ImportanceCalculationService importanceCalculationService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ReviewCardServiceImpl(ReviewCardMapper reviewCardMapper, KnowledgeNodeMapper knowledgeNodeMapper,
                             EbbinghausService ebbinghausService, QuestionGenerationService questionGenerationService,
                             ImportanceCalculationService importanceCalculationService, JdbcTemplate jdbcTemplate) {
        this.reviewCardMapper = reviewCardMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.ebbinghausService = ebbinghausService;
        this.questionGenerationService = questionGenerationService;
        this.importanceCalculationService = importanceCalculationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType) {
        return generateReviewCard(nodeId, cardType, "auto");
    }

    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", nodeId);
            return null;
        }

        ReviewCard card = questionGenerationService.generateHighQualityQuestion(node, cardType, node.getUserId());
        card.setGenerationType(generationType);

        reviewCardMapper.insert(card);
        log.info("生成复习卡片成功，nodeId：{}，cardType：{}，generationType：{}，cardId：{}", 
                nodeId, cardType, generationType, card.getId());
        
        return card;
    }

    @Override
    public List<ReviewCard> getTodayReviewCards(Long userId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getUserId, userId);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.eq(ReviewCard::getStatus, 0);
        wrapper.orderByAsc(ReviewCard::getNextReviewTime);

        List<ReviewCard> cards = reviewCardMapper.selectList(wrapper);
        
        log.info("获取待复习卡片，userId：{}，卡片数：{}（包含自动和手动生成的）", userId, cards.size());
        
        return cards;
    }

    @Override
    public ReviewCard getReviewCardById(Long id) {
        return reviewCardMapper.selectById(id);
    }

    @Override
    public ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration) {
        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null) {
            log.warn("复习卡片不存在，cardId：{}", cardId);
            return new ReviewResultDTO(false, "", null, "复习卡片不存在");
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
        
        // 无论自动生成还是手动生成，提交后都硬删除
        reviewCardMapper.deleteById(cardId);
        log.info("复习卡片已完成并硬删除，cardId：{}", cardId);
        
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

    private void syncToKnowledgeNode(ReviewCard card) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(card.getNodeId());
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", card.getNodeId());
            return;
        }

        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getNodeId, card.getNodeId());
        wrapper.eq(ReviewCard::getDeleted, 0);
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

    @Override
    public List<ReviewCard> getReviewCardsByNodeId(Long nodeId) {
        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getNodeId, nodeId);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.orderByAsc(ReviewCard::getCreateTime);

        return reviewCardMapper.selectList(wrapper);
    }

    @Override
    public void deleteReviewCard(Long id) {
        ReviewCard card = reviewCardMapper.selectById(id);
        if (card != null) {
            card.setDeleted(1);
            reviewCardMapper.updateById(card);
            log.info("删除复习卡片成功，cardId：{}", id);
        }
    }

    @Override
    public void deleteAllReviewCards(Long userId) {
        LambdaUpdateWrapper<ReviewCard> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(ReviewCard::getDeleted, 1);
        wrapper.eq(ReviewCard::getDeleted, 0);
        wrapper.eq(ReviewCard::getUserId, userId);
        int deletedCount = reviewCardMapper.update(null, wrapper);
        log.info("软删除所有复习卡片成功，userId：{}，共{}张", userId, deletedCount);
    }

    @Override
    public int restoreReviewCards(Long userId) {
        // 使用 JdbcTemplate 执行原生 SQL，绕过@TableLogic 的自动处理
        // 恢复卡片：将 deleted 设为 0，同时将 is_restored 设为 1
        String sql = "UPDATE review_card SET deleted = 0, is_restored = 1 WHERE user_id = ? AND deleted = 1";
        int count = jdbcTemplate.update(sql, userId);
        
        log.info("恢复复习卡片成功，userId：{}，共{}张", userId, count);
        return count;
    }

    @Override
    public int generateReviewCardsForAllNodes(Long userId) {
        log.info("开始为用户{}的所有知识点生成手动练习卡片", userId);
        
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getDeleted, 0)
                        .eq(KnowledgeNode::getUserId, userId)
        );

        int generatedCount = 0;
        for (KnowledgeNode node : allNodes) {
            try {
                for (int i = 0; i < 2; i++) {
                    ReviewCard card = generateReviewCard(node.getId(), "choice", "manual");
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

    @Override
    @Async("vectorTaskExecutor")
    public void generateReviewCardsForAllNodesAsync() {
        log.info("异步生成所有知识点的复习卡片");
        try {
            generateReviewCardsForAllNodes(1L);
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

    @Override
    public long countPendingByUserId(Long userId) {
        return reviewCardMapper.selectCount(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .eq(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
        );
    }

    @Override
    public long countCompletedByUserId(Long userId) {
        return reviewCardMapper.selectCount(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .gt(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
        );
    }

    @Override
    public long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return reviewCardMapper.selectCount(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .gt(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
                .ge(ReviewCard::getNextReviewTime, startTime)
                .lt(ReviewCard::getNextReviewTime, endTime)
        );
    }

    @Override
    public int calculateStreakDays(Long userId) {
        if (userId == null) {
            return 0;
        }

        int streakDays = 0;
        LocalDateTime currentDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        while (true) {
            LocalDateTime dayStart = currentDate;
            LocalDateTime dayEnd = currentDate.plusDays(1);

            long count = reviewCardMapper.selectCount(
                new LambdaQueryWrapper<ReviewCard>()
                    .eq(ReviewCard::getUserId, userId)
                    .gt(ReviewCard::getReviewCount, 0)
                    .eq(ReviewCard::getDeleted, 0)
                    .ge(ReviewCard::getLastReviewTime, dayStart)
                    .lt(ReviewCard::getLastReviewTime, dayEnd)
            );

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

    @Override
    public int getUserAccuracy(Long userId) {
        // 统计用户所有题目的答题情况
        List<ReviewCard> cards = reviewCardMapper.selectList(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .gt(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
        );

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
