package com.secondbrain.task;

import com.secondbrain.service.ReviewReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewScheduleTask {

    private static final Logger log = LoggerFactory.getLogger(ReviewScheduleTask.class);

    private final ReviewReminderService reviewReminderService;

    public ReviewScheduleTask(ReviewReminderService reviewReminderService) {
        this.reviewReminderService = reviewReminderService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scanPendingReviews() {
        log.info("开始扫描待复习知识点...");
        
        try {
            reviewReminderService.processExpiredReminders();
            log.info("扫描待复习知识点完成");
        } catch (Exception e) {
            log.error("扫描待复习知识点失败", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void checkReminders() {
        log.debug("检查复习提醒...");
        
        try {
            reviewReminderService.processExpiredReminders();
        } catch (Exception e) {
            log.error("检查复习提醒失败", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDailyReviewNotification() {
        log.info("发送每日复习提醒通知...");
        
        try {
            reviewReminderService.sendDailyReviewNotification();
            log.info("每日复习提醒通知发送完成");
        } catch (Exception e) {
            log.error("发送每日复习提醒通知失败", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void generateWeeklyReport() {
        log.info("生成每周复习报告...");
        
        try {
            reviewReminderService.generateWeeklyReport();
            log.info("每周复习报告生成完成");
        } catch (Exception e) {
            log.error("生成每周复习报告失败", e);
        }
    }
}
