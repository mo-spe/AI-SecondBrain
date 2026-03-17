package com.secondbrain.service;

import java.time.LocalDateTime;

public interface EbbinghausService {

    double calculateRetentionRate(int reviewCount);

    long calculateNextReviewInterval(int reviewCount, boolean isCorrect);

    LocalDateTime calculateNextReviewTime(LocalDateTime lastReviewTime, int reviewCount, boolean isCorrect);

    int calculateMasteryLevel(int reviewCount, double averageAccuracy);

    double calculateMemoryStrength(int reviewCount, double averageAccuracy);
}
