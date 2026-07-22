package com.secondbrain.service;

import java.time.LocalDateTime;

/**
 * 艾宾浩斯遗忘曲线服务接口.
 * <p>提供记忆保留率计算、复习间隔计算等功能</p>
 */
public interface EbbinghausService {

    /**
     * 计算记忆保留率.
     *
     * @param reviewCount 复习次数
     * @return 保留率（0-1）
     */
    double calculateRetentionRate(int reviewCount);

    /**
     * 计算下次复习间隔（分钟）.
     *
     * @param reviewCount 复习次数
     * @param isCorrect 是否正确
     * @return 间隔分钟数
     */
    long calculateNextReviewInterval(int reviewCount, boolean isCorrect);

    /**
     * 计算下次复习时间.
     *
     * @param lastReviewTime 上次复习时间
     * @param reviewCount 复习次数
     * @param isCorrect 是否正确
     * @return 下次复习时间
     */
    LocalDateTime calculateNextReviewTime(LocalDateTime lastReviewTime, int reviewCount, boolean isCorrect);

    /**
     * 计算掌握程度.
     *
     * @param reviewCount 复习次数
     * @param averageAccuracy 平均正确率
     * @return 掌握程度（0-5）
     */
    int calculateMasteryLevel(int reviewCount, double averageAccuracy);

    /**
     * 计算记忆强度.
     *
     * @param reviewCount 复习次数
     * @param averageAccuracy 平均正确率
     * @return 记忆强度（0-1）
     */
    double calculateMemoryStrength(int reviewCount, double averageAccuracy);
}
