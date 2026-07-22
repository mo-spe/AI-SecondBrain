package com.secondbrain.service;

/**
 * 复习提醒服务接口.
 * <p>提供复习提醒的调度、取消、处理等功能</p>
 */
public interface ReviewReminderService {

    /**
     * 调度复习提醒.
     *
     * @param nodeId 知识点ID
     * @param delayMinutes 延迟分钟数
     */
    void scheduleReminder(Long nodeId, long delayMinutes);

    /**
     * 取消复习提醒.
     *
     * @param nodeId 知识点ID
     */
    void cancelReminder(Long nodeId);

    /**
     * 获取下次提醒时间.
     *
     * @param nodeId 知识点ID
     * @return 下次提醒时间（时间戳）
     */
    Long getNextReminderTime(Long nodeId);

    /**
     * 处理过期提醒.
     */
    void processExpiredReminders();

    /**
     * 发送每日复习通知.
     */
    void sendDailyReviewNotification();

    /**
     * 生成周报.
     */
    void generateWeeklyReport();
}
