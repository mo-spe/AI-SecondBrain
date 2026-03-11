package com.secondbrain.service.impl;

import com.secondbrain.entity.ReviewCard;
import com.secondbrain.entity.User;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    @Autowired
    public NotificationServiceImpl(JavaMailSender mailSender, UserMapper userMapper) {
        this.mailSender = mailSender;
        this.userMapper = userMapper;
    }

    @Override
    public void sendDailyReviewNotification(Long userId, int pendingCount) {
        if (!mailEnabled) {
            log.debug("邮件服务未启用，跳过发送每日复习提醒");
            return;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getEmail() == null) {
                log.warn("用户不存在或邮箱为空，userId：{}", userId);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("【AI第二大脑】今日复习提醒");

            String content = buildDailyReviewEmailContent(user.getUsername(), pendingCount);
            message.setText(content);

            mailSender.send(message);
            log.info("每日复习提醒邮件发送成功，userId：{}，email：{}，pendingCount：{}",
                    userId, user.getEmail(), pendingCount);
        } catch (Exception e) {
            log.error("发送每日复习提醒邮件失败，userId：{}", userId, e);
        }
    }

    @Override
    public void sendReviewReminder(Long userId, String title, String nextReviewTime) {
        if (!mailEnabled) {
            log.debug("邮件服务未启用，跳过发送复习提醒");
            return;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getEmail() == null) {
                log.warn("用户不存在或邮箱为空，userId：{}", userId);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("【AI第二大脑】复习提醒");

            String content = buildReviewReminderEmailContent(user.getUsername(), title, nextReviewTime);
            message.setText(content);

            mailSender.send(message);
            log.info("复习提醒邮件发送成功，userId：{}，email：{}，title：{}",
                    userId, user.getEmail(), title);
        } catch (Exception e) {
            log.error("发送复习提醒邮件失败，userId：{}", userId, e);
        }
    }

    @Override
    public void sendWeeklyReport(Long userId, int totalReviews, double averageAccuracy, int masteredCards) {
        if (!mailEnabled) {
            log.debug("邮件服务未启用，跳过发送每周复习报告");
            return;
        }

        try {
            User user = userMapper.selectById(userId);
            if (user == null || user.getEmail() == null) {
                log.warn("用户不存在或邮箱为空，userId：{}", userId);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject("【AI第二大脑】每周复习报告");

            String content = buildWeeklyReportEmailContent(user.getUsername(), totalReviews, averageAccuracy, masteredCards);
            message.setText(content);

            mailSender.send(message);
            log.info("每周复习报告邮件发送成功，userId：{}，email：{}，totalReviews：{}，averageAccuracy：{}，masteredCards：{}",
                    userId, user.getEmail(), totalReviews, averageAccuracy, masteredCards);
        } catch (Exception e) {
            log.error("发送每周复习报告邮件失败，userId：{}", userId, e);
        }
    }

    private String buildDailyReviewEmailContent(String username, int pendingCount) {
        return String.format(
                "【AI第二大脑】今日复习提醒\n\n" +
                        "亲爱的 %s，今天有新的复习任务等待你完成！\n\n" +
                        "复习统计：\n" +
                        "- 待复习卡片数：%d 张\n\n" +
                        "根据艾宾浩斯遗忘曲线，及时复习可以显著提高记忆效果。\n" +
                        "建议您在空闲时进行复习，每次复习时间控制在 5-10 分钟。\n\n" +
                        "请登录系统开始复习：http://localhost:3000/review\n\n" +
                        "此邮件由 AI 第二大脑自动发送，请勿回复。\n" +
                        "© 2026 AI 第二大脑 - 让知识不再遗忘",
                username, pendingCount
        );
    }

    private String buildReviewReminderEmailContent(String username, String title, String nextReviewTime) {
        return String.format(
                "【AI第二大脑】复习提醒\n\n" +
                        "亲爱的 %s，是时候复习了！\n\n" +
                        "题目：%s\n" +
                        "下次复习时间：%s\n\n" +
                        "及时复习有助于巩固记忆，避免遗忘。\n" +
                        "建议您在合适的时间进行复习，保持良好的学习节奏。\n\n" +
                        "此邮件由 AI 第二大脑自动发送，请勿回复。\n" +
                        "© 2026 AI 第二大脑 - 让知识不再遗忘",
                username, title, nextReviewTime
        );
    }

    private String buildWeeklyReportEmailContent(String username, int totalReviews, double averageAccuracy, int masteredCards) {
        return String.format(
                "【AI第二大脑】每周复习报告\n\n" +
                        "亲爱的 %s，这是你本周的复习统计报告\n\n" +
                        "复习统计：\n" +
                        "- 总复习次数：%d\n" +
                        "- 平均准确率：%.1f%%\n" +
                        "- 已掌握卡片：%d\n\n" +
                        "继续保持良好的复习习惯，让知识真正成为你的财富！\n\n" +
                        "此邮件由 AI 第二大脑自动发送，请勿回复。\n" +
                        "© 2026 AI 第二大脑 - 让知识不再遗忘",
                username, totalReviews, averageAccuracy * 100, masteredCards
        );
    }
}
