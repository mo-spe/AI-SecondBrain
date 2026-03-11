package com.secondbrain.service;

import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.ReviewCard;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewCardService {

    ReviewCard generateReviewCard(Long nodeId, String cardType);

    List<ReviewCard> getTodayReviewCards(Long userId);

    ReviewCard getReviewCardById(Long id);

    ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration);

    void updateReviewSchedule(Long cardId, boolean isCorrect);

    List<ReviewCard> getReviewCardsByNodeId(Long nodeId);

    void deleteReviewCard(Long id);
    
    void deleteAllReviewCards();
    
    int generateReviewCardsForAllNodes();
    
    void updateMissingAnswers();

    long countPendingByUserId(Long userId);

    long countCompletedByUserId(Long userId);

    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    int calculateStreakDays(Long userId);
}
