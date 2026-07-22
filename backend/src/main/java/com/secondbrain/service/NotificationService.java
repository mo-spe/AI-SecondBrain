package com.secondbrain.service;

/**
 * 通知服务接口.
 * <p>提供复习提醒、周报等通知功能</p>
 */
public interface NotificationService {

    /**
     * 发送每日复习通知.
     *
     * @param userId 用户ID
     * @param pendingCount 待复习数量
     */
    void sendDailyReviewNotification(Long userId, int pendingCount);

    /**
     * 发送复习提醒.
     *
     * @param userId 用户ID
     * @param title 标题
     * @param nextReviewTime 下次复习时间
     */
    void sendReviewReminder(Long userId, String title, String nextReviewTime);

    /**
     * 发送周报.
     *
     * @param userId 用户ID
     * @param totalReviews 总复习次数
     * @param averageAccuracy 平均正确率
     * @param masteredCards 已掌握卡片数
     */
    void sendWeeklyReport(Long userId, int totalReviews, double averageAccuracy, int masteredCards);
}
