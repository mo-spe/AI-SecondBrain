package com.secondbrain.service;

import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.ReviewCard;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewCardService {

    ReviewCard generateReviewCard(Long nodeId, String cardType);

    ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType);

    List<ReviewCard> getTodayReviewCards(Long userId);

    ReviewCard getReviewCardById(Long id);

    ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration);

    void updateReviewSchedule(Long cardId, boolean isCorrect);

    List<ReviewCard> getReviewCardsByNodeId(Long nodeId);

    void deleteReviewCard(Long id);
    
    void deleteAllReviewCards(Long userId);
    
    int generateReviewCardsForAllNodes(Long userId);
    
    void generateReviewCardsForAllNodesAsync();
    
    int restoreReviewCards(Long userId);
    
    void updateMissingAnswers();

    long countPendingByUserId(Long userId);

    long countCompletedByUserId(Long userId);

    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    int calculateStreakDays(Long userId);

    void recordQualityFeedback(Long cardId, Integer rating, String comment);
    
    /**
     * 获取用户全局准确率
     * @param userId 用户 ID
     * @return 准确率（0-100 之间的整数）
     */
    int getUserAccuracy(Long userId);
}
