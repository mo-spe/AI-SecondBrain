package com.secondbrain.service;

import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.ReviewCard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 复习卡片服务接口.
 * <p>提供复习卡片的生成、查询、提交、统计等功能</p>
 */
public interface ReviewCardService {

    /**
     * 生成复习卡片.
     *
     * @param nodeId 知识点ID
     * @param cardType 卡片类型
     * @return 复习卡片
     */
    ReviewCard generateReviewCard(Long nodeId, String cardType);

    /**
     * 生成复习卡片（带生成类型）.
     *
     * @param nodeId 知识点ID
     * @param cardType 卡片类型
     * @param generationType 生成类型
     * @return 复习卡片
     */
    ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType);

    /**
     * 获取今日待复习卡片列表.
     *
     * @param userId 用户ID
     * @return 复习卡片列表
     */
    List<ReviewCard> getTodayReviewCards(Long userId);

    /**
     * 根据ID获取复习卡片.
     *
     * @param id 卡片ID
     * @return 复习卡片
     */
    ReviewCard getReviewCardById(Long id);

    /**
     * 提交复习结果.
     *
     * @param cardId 卡片ID
     * @param userAnswer 用户答案
     * @param duration 答题时长（秒）
     * @return 复习结果
     */
    ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration);

    /**
     * 更新复习计划.
     *
     * @param cardId 卡片ID
     * @param isCorrect 是否正确
     */
    void updateReviewSchedule(Long cardId, boolean isCorrect);

    /**
     * 获取知识点的复习卡片列表.
     *
     * @param nodeId 知识点ID
     * @return 复习卡片列表
     */
    List<ReviewCard> getReviewCardsByNodeId(Long nodeId);

    /**
     * 删除复习卡片.
     *
     * @param id 卡片ID
     */
    void deleteReviewCard(Long id);

    /**
     * 删除用户所有复习卡片.
     *
     * @param userId 用户ID
     */
    void deleteAllReviewCards(Long userId);

    /**
     * 为所有知识点生成复习卡片.
     *
     * @param userId 用户ID
     * @return 生成数量
     */
    int generateReviewCardsForAllNodes(Long userId);

    /**
     * 异步为所有知识点生成复习卡片.
     */
    void generateReviewCardsForAllNodesAsync();

    /**
     * 恢复复习卡片.
     *
     * @param userId 用户ID
     * @return 恢复数量
     */
    int restoreReviewCards(Long userId);

    /**
     * 更新缺失答案的卡片.
     */
    void updateMissingAnswers();

    /**
     * 统计待复习卡片数量.
     *
     * @param userId 用户ID
     * @return 待复习数量
     */
    long countPendingByUserId(Long userId);

    /**
     * 统计已完成卡片数量.
     *
     * @param userId 用户ID
     * @return 已完成数量
     */
    long countCompletedByUserId(Long userId);

    /**
     * 统计指定时间范围内的复习卡片数量.
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 复习卡片数量
     */
    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 计算连续打卡天数.
     *
     * @param userId 用户ID
     * @return 连续天数
     */
    int calculateStreakDays(Long userId);

    /**
     * 记录质量反馈.
     *
     * @param cardId 卡片ID
     * @param rating 评分
     * @param comment 评论
     */
    void recordQualityFeedback(Long cardId, Integer rating, String comment);

    /**
     * 获取用户全局准确率.
     *
     * @param userId 用户ID
     * @return 准确率（0-100）
     */
    int getUserAccuracy(Long userId);
}
