package com.secondbrain.test;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.ReviewCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class TestReviewFunction {

    @Autowired
    private EbbinghausService ebbinghausService;

    @Autowired
    private ReviewCardService reviewCardService;

    @Autowired
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Autowired
    private ReviewCardMapper reviewCardMapper;

    @Test
    public void testEbbinghausAlgorithm() {
        System.out.println("=== 测试艾宾浩斯遗忘曲线算法 ===");

        System.out.println("\n1. 计算记忆保留率");
        for (int i = 0; i <= 10; i++) {
            double retentionRate = ebbinghausService.calculateRetentionRate(i);
            System.out.println("   复习次数" + i + "：记忆保留率 = " + String.format("%.4f", retentionRate));
        }

        System.out.println("\n2. 计算下次复习间隔");
        for (int i = 0; i <= 10; i++) {
            long interval = ebbinghausService.calculateNextReviewInterval(i, true);
            System.out.println("   复习次数" + i + "：下次复习间隔 = " + interval + "分钟");
        }

        System.out.println("\n3. 计算掌握程度");
        for (int reviewCount = 0; reviewCount <= 10; reviewCount++) {
            for (double accuracy = 0.5; accuracy <= 1.0; accuracy += 0.1) {
                int masteryLevel = ebbinghausService.calculateMasteryLevel(reviewCount, accuracy);
                System.out.println("   复习" + reviewCount + "次，准确率" + String.format("%.1f", accuracy) + 
                        "：掌握程度 = " + masteryLevel);
            }
        }

        System.out.println("\n4. 计算记忆强度");
        for (int reviewCount = 0; reviewCount <= 10; reviewCount++) {
            for (double accuracy = 0.5; accuracy <= 1.0; accuracy += 0.1) {
                double strength = ebbinghausService.calculateMemoryStrength(reviewCount, accuracy);
                System.out.println("   复习" + reviewCount + "次，准确率" + String.format("%.1f", accuracy) + 
                        "：记忆强度 = " + String.format("%.4f", strength));
            }
        }
    }

    @Test
    public void testGenerateReviewCard() {
        System.out.println("=== 测试生成复习卡片 ===");

        Long userId = 5L;

        List<KnowledgeNode> nodes = knowledgeNodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                        .eq(KnowledgeNode::getUserId, userId)
                        .eq(KnowledgeNode::getDeleted, 0)
                        .last("LIMIT 3")
        );

        System.out.println("找到" + nodes.size() + "个知识点");

        for (KnowledgeNode node : nodes) {
            System.out.println("\n知识点：" + node.getTitle());

            ReviewCard simpleCard = reviewCardService.generateReviewCard(node.getId(), "simple");
            if (simpleCard != null) {
                System.out.println("  简单卡片生成成功，ID：" + simpleCard.getId());
                System.out.println("  问题：" + simpleCard.getQuestion());
            }

            ReviewCard choiceCard = reviewCardService.generateReviewCard(node.getId(), "choice");
            if (choiceCard != null) {
                System.out.println("  单选题卡片生成成功，ID：" + choiceCard.getId());
                System.out.println("  问题：" + choiceCard.getQuestion());
            }

            ReviewCard fillCard = reviewCardService.generateReviewCard(node.getId(), "fill");
            if (fillCard != null) {
                System.out.println("  填空题卡片生成成功，ID：" + fillCard.getId());
                System.out.println("  问题：" + fillCard.getQuestion());
            }
        }
    }

    @Test
    public void testReviewProcess() {
        System.out.println("=== 测试复习流程 ===");

        Long userId = 5L;

        List<ReviewCard> cards = reviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                        .eq(ReviewCard::getUserId, userId)
                        .eq(ReviewCard::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (cards.isEmpty()) {
            System.out.println("没有找到复习卡片，请先生成卡片");
            return;
        }

        ReviewCard card = cards.get(0);
        System.out.println("卡片ID：" + card.getId());
        System.out.println("问题：" + card.getQuestion());
        System.out.println("当前复习次数：" + card.getReviewCount());
        System.out.println("当前准确率：" + String.format("%.4f", card.getAverageAccuracy()));

        System.out.println("\n模拟复习（正确）");
        reviewCardService.submitReviewResult(card.getId(), "A", 30);

        ReviewCard updatedCard = reviewCardMapper.selectById(card.getId());
        System.out.println("复习后复习次数：" + updatedCard.getReviewCount());
        System.out.println("复习后准确率：" + String.format("%.4f", updatedCard.getAverageAccuracy()));
        System.out.println("掌握程度：" + updatedCard.getMasteryLevel());
        System.out.println("记忆强度：" + String.format("%.4f", updatedCard.getMemoryStrength()));
        System.out.println("下次复习时间：" + updatedCard.getNextReviewTime());

        System.out.println("\n模拟复习（错误）");
        reviewCardService.submitReviewResult(card.getId(), "B", 20);

        ReviewCard updatedCard2 = reviewCardMapper.selectById(card.getId());
        System.out.println("复习后复习次数：" + updatedCard2.getReviewCount());
        System.out.println("复习后准确率：" + String.format("%.4f", updatedCard2.getAverageAccuracy()));
        System.out.println("掌握程度：" + updatedCard2.getMasteryLevel());
        System.out.println("记忆强度：" + String.format("%.4f", updatedCard2.getMemoryStrength()));
        System.out.println("下次复习时间：" + updatedCard2.getNextReviewTime());
    }

    @Test
    public void testTodayReviewCards() {
        System.out.println("=== 测试获取今日复习卡片 ===");

        Long userId = 5L;

        List<ReviewCard> todayCards = reviewCardService.getTodayReviewCards(userId);

        System.out.println("今日待复习卡片数：" + todayCards.size());

        for (int i = 0; i < todayCards.size(); i++) {
            ReviewCard card = todayCards.get(i);
            System.out.println("\n" + (i + 1) + ". 卡片ID：" + card.getId());
            System.out.println("   问题：" + card.getQuestion());
            System.out.println("   复习次数：" + card.getReviewCount());
            System.out.println("   准确率：" + String.format("%.4f", card.getAverageAccuracy()));
            System.out.println("   掌握程度：" + card.getMasteryLevel());
            System.out.println("   下次复习时间：" + card.getNextReviewTime());
        }
    }

    @Test
    public void testReviewSchedule() {
        System.out.println("=== 测试复习计划 ===");

        Long userId = 5L;

        List<ReviewCard> cards = reviewCardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewCard>()
                        .eq(ReviewCard::getUserId, userId)
                        .eq(ReviewCard::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (cards.isEmpty()) {
            System.out.println("没有找到复习卡片");
            return;
        }

        ReviewCard card = cards.get(0);
        System.out.println("卡片ID：" + card.getId());

        LocalDateTime now = LocalDateTime.now();
        System.out.println("当前时间：" + now);

        for (int i = 0; i <= 5; i++) {
            LocalDateTime nextReviewTime = ebbinghausService.calculateNextReviewTime(now, i, true);
            System.out.println("复习" + i + "次后，下次复习时间：" + nextReviewTime);
        }

        for (int i = 0; i <= 5; i++) {
            LocalDateTime nextReviewTime = ebbinghausService.calculateNextReviewTime(now, i, false);
            System.out.println("复习" + i + "次后（错误），下次复习时间：" + nextReviewTime);
        }
    }
}
