package com.secondbrain.service;

public interface NotificationService {

    void sendDailyReviewNotification(Long userId, int pendingCount);

    void sendReviewReminder(Long userId, String title, String nextReviewTime);

    void sendWeeklyReport(Long userId, int totalReviews, double averageAccuracy, int masteredCards);
}
