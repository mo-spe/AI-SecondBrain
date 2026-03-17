package com.secondbrain.service;

public interface ReviewReminderService {

    void scheduleReminder(Long nodeId, long delayMinutes);

    void cancelReminder(Long nodeId);

    Long getNextReminderTime(Long nodeId);

    void processExpiredReminders();

    void sendDailyReviewNotification();

    void generateWeeklyReport();
}
