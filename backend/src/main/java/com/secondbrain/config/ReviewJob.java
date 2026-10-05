package com.secondbrain.config;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.entity.UserReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.mapper.UserReviewCardMapper;
import com.secondbrain.service.ReviewCardService;
import com.secondbrain.service.NotificationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 复习任务定时作业. <p>负责自动生成复习卡片并触发复习提醒</p> */
@Component
public class ReviewJob extends QuartzJobBean {

    private static final Logger log = LoggerFactory.getLogger(ReviewJob.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReviewCardService reviewCardService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final ReviewCardMapper reviewCardMapper;
    private final UserReviewCardMapper userReviewCardMapper;
    private final NotificationService notificationService;

    /**
     * 构造器注入依赖.
     *
     * @param reviewCardService   复习卡片服务
     * @param knowledgeNodeMapper 知识点 Mapper
     * @param reviewCardMapper    复习卡片 Mapper
     * @param userReviewCardMapper 用户复习副本 Mapper
     * @param notificationService 通知服务
     */
    public ReviewJob(ReviewCardService reviewCardService,
                     KnowledgeNodeMapper knowledgeNodeMapper,
                     ReviewCardMapper reviewCardMapper,
                     UserReviewCardMapper userReviewCardMapper,
                     NotificationService notificationService) {
        this.reviewCardService = reviewCardService;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.reviewCardMapper = reviewCardMapper;
        this.userReviewCardMapper = userReviewCardMapper;
        this.notificationService = notificationService;
    }

    /**
     * 执行复习任务调度.
     *
     * @param context 作业执行上下文
     * @return void
     * @throws JobExecutionException 作业执行异常
     */
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

        LocalDateTime now = LocalDateTime.now();

        List<KnowledgeNode> nodesNeedReview = knowledgeNodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                        .le(KnowledgeNode::getNextReviewTime, now)
                        .eq(KnowledgeNode::getDeleted, 0)
        );

        int generatedCount = 0;
        
        for (KnowledgeNode node : nodesNeedReview) {
            try {
                // 复习排期现在由个人卡片的 nextReviewTime 驱动；知识点到期时若已有卡片，
                // 只需让原卡片重新进入队列，不能再次生成题目造成重复。
                List<ReviewCard> existingCards = reviewCardService.getReviewCardsByNodeId(
                        node.getId(), node.getUserId(), node.getWorkspaceId());
                if (existingCards != null && !existingCards.isEmpty()) {
                    log.debug("知识点已有复习卡片，跳过重复生成 nodeId={} count={}", node.getId(), existingCards.size());
                    continue;
                }
                ReviewCard card = reviewCardService.generateReviewCard(node.getId(), "choice", "auto", node.getUserId());
                if (card != null) {
                    generatedCount++;
                    log.info("为知识点生成复习卡片成功，nodeId：{}，nextReviewTime：{}，cardId：{}", 
                            node.getId(), node.getNextReviewTime(), card.getId());
                }
            } catch (Exception e) {
                log.error("生成复习卡片失败，nodeId：{}", node.getId(), e);
            }
        }

        log.info("自动生成复习卡片完成，需要复习的知识点数：{}，生成{}张卡片", 
                nodesNeedReview.size(), generatedCount);
    }

    private void sendDailyReviewNotifications() {
        log.info("开始发送每日复习提醒");

        LocalDateTime now = LocalDateTime.now();
        java.util.Map<Long, Integer> userPendingCounts = new java.util.HashMap<>();

        // 新题目池模型的排期保存在用户个人副本中，必须和旧表一起统计才能覆盖全部用户。
        List<UserReviewCard> personalCards = userReviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserReviewCard>()
                        .eq(UserReviewCard::getIsArchived, 0)
                        .and(status -> status.eq(UserReviewCard::getStatus, 0)
                                .or()
                                .eq(UserReviewCard::getStatus, 1))
                        .and(due -> due.isNull(UserReviewCard::getNextReviewTime)
                                .or()
                                .le(UserReviewCard::getNextReviewTime, now))
        );
        for (UserReviewCard card : personalCards) {
            userPendingCounts.merge(card.getUserId(), 1, Integer::sum);
        }

        // 旧表继续保留兼容查询，避免存量卡片在迁移期间漏掉提醒。
        List<ReviewCard> todayCards = reviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                        .and(status -> status.eq(ReviewCard::getStatus, 0)
                                .or()
                                .eq(ReviewCard::getStatus, 1))
                        .eq(ReviewCard::getDeleted, 0)
                        .and(due -> due.isNull(ReviewCard::getNextReviewTime)
                                .or()
                                .le(ReviewCard::getNextReviewTime, now))
        );
        for (ReviewCard card : todayCards) {
            userPendingCounts.merge(card.getUserId(), 1, Integer::sum);
        }

        for (java.util.Map.Entry<Long, Integer> entry : userPendingCounts.entrySet()) {
            Long userId = entry.getKey();
            int pendingCount = entry.getValue();

            log.info("用户{}今日待复习卡片数：{}", userId, pendingCount);

            notificationService.sendDailyReviewNotification(userId, pendingCount);
        }

        log.info("每日复习提醒发送完成，共{}个用户", userPendingCounts.size());
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
