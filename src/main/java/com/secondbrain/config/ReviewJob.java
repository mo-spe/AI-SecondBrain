package com.secondbrain.config;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.NotificationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReviewJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(ReviewJob.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private ReviewCardService reviewCardService;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private ReviewCardMapper reviewCardMapper;

    @Autowired
    private NotificationService notificationService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行复习任务调度，当前时间：{}", LocalDateTime.now());

        try {
            autoGenerateReviewCards();
            sendDailyReviewNotifications();
            generateWeeklyReport();

            log.info("复习任务调度完成，当前时间：{}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("复习任务调度失败", e);
            throw new JobExecutionException(e);
        }
    }

    private void autoGenerateReviewCards() {
        log.info("开始自动生成复习卡片");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<KnowledgeNode> recentNodes = knowledgeNodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                        .ge(KnowledgeNode::getCreateTime, yesterday)
                        .lt(KnowledgeNode::getCreateTime, today)
                        .eq(KnowledgeNode::getDeleted, 0)
        );

        int generatedCount = 0;
        for (KnowledgeNode node : recentNodes) {
            try {
                List<ReviewCard> existingCards = reviewCardMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                                .eq(ReviewCard::getNodeId, node.getId())
                                .eq(ReviewCard::getDeleted, 0)
                );

                if (existingCards.isEmpty()) {
                    ReviewCard card = reviewCardService.generateReviewCard(node.getId(), "simple");
                    if (card != null) {
                        generatedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("生成复习卡片失败，nodeId：{}", node.getId(), e);
            }
        }

        log.info("自动生成复习卡片完成，共生成{}张卡片", generatedCount);
    }

    private void sendDailyReviewNotifications() {
        log.info("开始发送每日复习提醒");

        List<ReviewCard> todayCards = reviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                        .eq(ReviewCard::getStatus, 0)
                        .eq(ReviewCard::getDeleted, 0)
                        .le(ReviewCard::getNextReviewTime, LocalDateTime.now())
        );

        java.util.Map<Long, List<ReviewCard>> userCards = new java.util.HashMap<>();
        for (ReviewCard card : todayCards) {
            userCards.computeIfAbsent(card.getUserId(), k -> new java.util.ArrayList<>()).add(card);
        }

        for (java.util.Map.Entry<Long, List<ReviewCard>> entry : userCards.entrySet()) {
            Long userId = entry.getKey();
            List<ReviewCard> cards = entry.getValue();

            log.info("用户{}今日待复习卡片数：{}", userId, cards.size());

            notificationService.sendDailyReviewNotification(userId, cards.size());

            for (ReviewCard card : cards) {
                log.debug("卡片ID：{}，下次复习时间：{}", card.getId(), card.getNextReviewTime());
            }
        }

        log.info("每日复习提醒发送完成，共{}个用户", userCards.size());
    }

    private void generateWeeklyReport() {
        log.info("开始生成每周复习报告");

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime now = LocalDateTime.now();

        List<ReviewCard> weeklyReviews = reviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                        .ge(ReviewCard::getLastReviewTime, weekAgo)
                        .le(ReviewCard::getLastReviewTime, now)
                        .eq(ReviewCard::getDeleted, 0)
        );

        java.util.Map<Long, List<ReviewCard>> userReviews = new java.util.HashMap<>();
        for (ReviewCard card : weeklyReviews) {
            userReviews.computeIfAbsent(card.getUserId(), k -> new java.util.ArrayList<>()).add(card);
        }

        for (java.util.Map.Entry<Long, List<ReviewCard>> entry : userReviews.entrySet()) {
            Long userId = entry.getKey();
            List<ReviewCard> cards = entry.getValue();

            int totalReviews = cards.stream()
                    .mapToInt(ReviewCard::getReviewCount)
                    .sum();

            int totalCorrect = cards.stream()
                    .mapToInt(ReviewCard::getCorrectCount)
                    .sum();

            double averageAccuracy = totalReviews > 0 ? (double) totalCorrect / totalReviews : 0.0;

            long masteredCards = cards.stream()
                    .filter(card -> card.getMasteryLevel() >= 4)
                    .count();

            log.info("用户{}本周复习统计：总复习{}次，正确率{:.2f}%，掌握{}张卡片",
                    userId, totalReviews, averageAccuracy * 100, masteredCards);

            notificationService.sendWeeklyReport(userId, totalReviews, averageAccuracy, (int) masteredCards);
        }

        log.info("每周复习报告生成完成");
    }
}
