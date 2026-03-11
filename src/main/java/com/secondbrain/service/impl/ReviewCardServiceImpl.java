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

    @Autowired
    public ReviewCardServiceImpl(ReviewCardMapper reviewCardMapper, KnowledgeNodeMapper knowledgeNodeMapper,
                             EbbinghausService ebbinghausService, QuestionGenerationService questionGenerationService,
                             ImportanceCalculationService importanceCalculationService) {
        this.reviewCardMapper = reviewCardMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.ebbinghausService = ebbinghausService;
        this.questionGenerationService = questionGenerationService;
        this.importanceCalculationService = importanceCalculationService;
    }

    @Override
    public ReviewCard generateReviewCard(Long nodeId, String cardType) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            log.warn("知识点不存在，nodeId：{}", nodeId);
            return null;
        }

        ReviewCard card = questionGenerationService.generateHighQualityQuestion(node, cardType);

        reviewCardMapper.insert(card);
        log.info("生成复习卡片成功，nodeId：{}，cardType：{}，cardId：{}", nodeId, cardType, card.getId());
        
        return card;
    }

    @Override
    public List<ReviewCard> getTodayReviewCards(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);

        LambdaQueryWrapper<ReviewCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewCard::getUserId, userId);
        wrapper.eq(ReviewCard::getDeleted, 0);
        
        wrapper.and(w -> {
            w.ge(ReviewCard::getNextReviewTime, todayStart)
             .lt(ReviewCard::getNextReviewTime, todayEnd)
             .or()
             .lt(ReviewCard::getNextReviewTime, todayStart);
        });
        
        wrapper.orderByAsc(ReviewCard::getNextReviewTime);

        List<ReviewCard> cards = reviewCardMapper.selectList(wrapper);
        
        log.info("获取今日待复习卡片，userId：{}，卡片数：{}", userId, cards.size());
        
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

        int totalReviews = card.getCorrectCount() + card.getIncorrectCount();
        double averageAccuracy = totalReviews > 0 ? (double) card.getCorrectCount() / totalReviews : 0.0;
        card.setAverageAccuracy(averageAccuracy);

        int masteryLevel = ebbinghausService.calculateMasteryLevel(card.getReviewCount(), averageAccuracy);
        card.setMasteryLevel(masteryLevel);

        double memoryStrength = ebbinghausService.calculateMemoryStrength(card.getReviewCount(), averageAccuracy);
        card.setMemoryStrength(memoryStrength);

        card.setLastReviewTime(LocalDateTime.now());
        
        card.setStatus(1);

        reviewCardMapper.updateById(card);

        updateReviewSchedule(cardId, isCorrect);

        syncToKnowledgeNode(card);

        log.info("提交复习结果成功，cardId：{}，isCorrect：{}，duration：{}秒，reviewCount：{}，accuracy：{}，masteryLevel：{}",
                cardId, isCorrect, duration, card.getReviewCount(), averageAccuracy, masteryLevel);

        String explanation = extractExplanation(card.getQuestion());
        String message = isCorrect ? "回答正确！继续保持！" : "回答错误，正确答案是：" + card.getAnswer();
        return new ReviewResultDTO(isCorrect, card.getAnswer(), explanation, message);
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
    public void deleteAllReviewCards() {
        LambdaUpdateWrapper<ReviewCard> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(ReviewCard::getDeleted, 1);
        wrapper.eq(ReviewCard::getDeleted, 0);
        reviewCardMapper.update(null, wrapper);
        log.info("删除所有复习卡片成功");
    }

    @Override
    public int generateReviewCardsForAllNodes() {
        log.info("开始为所有知识点生成复习卡片");
        
        List<KnowledgeNode> allNodes = knowledgeNodeMapper.selectList(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getDeleted, 0)
        );

        int generatedCount = 0;
        for (KnowledgeNode node : allNodes) {
            try {
                List<ReviewCard> existingCards = reviewCardMapper.selectList(
                        new LambdaQueryWrapper<ReviewCard>()
                                .eq(ReviewCard::getNodeId, node.getId())
                                .eq(ReviewCard::getDeleted, 0)
                );

                if (existingCards.isEmpty()) {
                    String[] cardTypes = {"simple", "choice"};
                    for (String cardType : cardTypes) {
                        ReviewCard card = generateReviewCard(node.getId(), cardType);
                        if (card != null) {
                            generatedCount++;
                            log.info("生成复习卡片成功，nodeId：{}，cardType：{}，cardId：{}", 
                                    node.getId(), cardType, card.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("生成复习卡片失败，nodeId：{}", node.getId(), e);
            }
        }

        log.info("为所有知识点生成复习卡片完成，共生成{}张卡片", generatedCount);
        return generatedCount;
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
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        return reviewCardMapper.selectCount(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .eq(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
                .ge(ReviewCard::getNextReviewTime, todayStart)
                .lt(ReviewCard::getNextReviewTime, todayEnd)
        );
    }

    @Override
    public long countCompletedByUserId(Long userId) {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        return reviewCardMapper.selectCount(
            new LambdaQueryWrapper<ReviewCard>()
                .eq(ReviewCard::getUserId, userId)
                .gt(ReviewCard::getReviewCount, 0)
                .eq(ReviewCard::getDeleted, 0)
                .ge(ReviewCard::getNextReviewTime, todayStart)
                .lt(ReviewCard::getNextReviewTime, todayEnd)
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
}
