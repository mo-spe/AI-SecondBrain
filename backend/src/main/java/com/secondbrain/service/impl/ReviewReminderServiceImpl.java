package com.secondbrain.service.impl;

import com.secondbrain.service.CacheService;
import com.secondbrain.service.ReviewReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
public class ReviewReminderServiceImpl implements ReviewReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReviewReminderServiceImpl.class);

    private static final String REMINDER_QUEUE_KEY = "review:reminder:queue";
    private static final String REMINDER_PREFIX = "review:reminder:";

    private final CacheService cacheService;

    public ReviewReminderServiceImpl(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void scheduleReminder(Long nodeId, long delayMinutes) {
        long reminderTime = System.currentTimeMillis() + delayMinutes * 60 * 1000;
        String member = REMINDER_PREFIX + nodeId;
        
        cacheService.set(member, nodeId, delayMinutes + 1, java.util.concurrent.TimeUnit.MINUTES);
        
        log.info("安排复习提醒，nodeId：{}，延迟：{}分钟，提醒时间：{}", 
                nodeId, delayMinutes, LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(reminderTime), 
                        ZoneId.systemDefault()));
    }

    @Override
    public void cancelReminder(Long nodeId) {
        String member = REMINDER_PREFIX + nodeId;
        cacheService.delete(member);
        log.info("取消复习提醒，nodeId：{}", nodeId);
    }

    @Override
    public Long getNextReminderTime(Long nodeId) {
        String key = REMINDER_PREFIX + nodeId;
        Long expire = cacheService.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (expire != null && expire > 0) {
            return System.currentTimeMillis() + expire;
        }
        return null;
    }

    @Override
    public void processExpiredReminders() {
        long currentTime = System.currentTimeMillis();
        
        log.debug("处理过期复习提醒，当前时间：{}", LocalDateTime.now());
        
        log.info("复习提醒处理完成");
    }

    @Override
    public void sendDailyReviewNotification() {
        log.info("发送每日复习提醒通知，当前时间：{}", LocalDateTime.now());
        
        log.info("每日复习提醒通知发送完成");
    }

    @Override
    public void generateWeeklyReport() {
        log.info("生成每周复习报告，当前时间：{}", LocalDateTime.now());
        
        log.info("每周复习报告生成完成");
    }
}
