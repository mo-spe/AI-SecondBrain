package com.secondbrain.service;

import com.secondbrain.dto.ReviewResultDTO;
import com.secondbrain.entity.ReviewCard;

import java.time.LocalDateTime;
import java.util.List;

/** 复习卡片服务接口. <p>提供复习卡片的生成、查询、提交复习结果及统计功能</p> */
public interface ReviewCardService {

    /**
     * 生成复习卡片.
     *
     * @param nodeId 知识节点ID
     * @param cardType 卡片类型
     * @param userId 当前用户ID（卡片归属用户）
     * @return 复习卡片
     */
    ReviewCard generateReviewCard(Long nodeId, String cardType, Long userId);

    /**
     * 生成复习卡片（指定生成方式）.
     *
     * @param nodeId 知识节点ID
     * @param cardType 卡片类型
     * @param generationType 生成方式
     * @param userId 当前用户ID（卡片归属用户）
     * @return 复习卡片
     */
    ReviewCard generateReviewCard(Long nodeId, String cardType, String generationType, Long userId);

    /**
     * 获取今日待复习卡片列表.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 复习卡片列表
     */
    List<ReviewCard> getTodayReviewCards(Long userId, Long workspaceId);

    /**
     * 根据ID查询复习卡片.
     *
     * @param id 卡片ID
     * @return 复习卡片
     */
    ReviewCard getReviewCardById(Long id);

    /**
     * 提交复习结果.
     *
     * @param cardId     卡片ID
     * @param userAnswer 用户答案
     * @param duration   答题时长
     * @param userId     当前用户ID（用于权限校验）
     * @return 复习结果
     */
    ReviewResultDTO submitReviewResult(Long cardId, String userAnswer, Integer duration, Long userId);

    /**
     * 更新复习计划.
     *
     * @param cardId 卡片ID
     * @param isCorrect 是否答对
     * @return void
     */
    void updateReviewSchedule(Long cardId, boolean isCorrect);

    /**
     * 根据知识节点ID查询复习卡片列表（含用户隔离）.
     *
     * @param nodeId      知识节点ID
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 复习卡片列表
     */
    List<ReviewCard> getReviewCardsByNodeId(Long nodeId, Long userId, Long workspaceId);

    /**
     * 删除复习卡片.
     *
     * @param id     卡片ID
     * @param userId 当前用户ID（用于权限校验）
     * @return void
     */
    void deleteReviewCard(Long id, Long userId);

    /**
     * 删除用户全部复习卡片.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void deleteAllReviewCards(Long userId, Long workspaceId);

    /**
     * 为用户所有知识节点生成复习卡片.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 生成数量
     */
    int generateReviewCardsForAllNodes(Long userId, Long workspaceId);

    /**
     * 异步为所有知识节点生成复习卡片.
     *
     * @return void
     */
    void generateReviewCardsForAllNodesAsync();

    /**
     * 恢复用户复习卡片.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 恢复数量
     */
    int restoreReviewCards(Long userId, Long workspaceId);

    /**
     * 更新缺失答案的复习卡片.
     *
     * @return void
     */
    void updateMissingAnswers();

    /**
     * 统计用户待复习卡片数量.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 待复习数量
     */
    long countPendingByUserId(Long userId, Long workspaceId);

    /**
     * 统计用户已完成卡片数量.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 已完成数量
     */
    long countCompletedByUserId(Long userId, Long workspaceId);

    /**
     * 统计指定时间范围内的卡片数量.
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param workspaceId 工作区ID
     * @return 卡片数量
     */
    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId);

    /**
     * 计算用户连续复习天数.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 连续天数
     */
    int calculateStreakDays(Long userId, Long workspaceId);

    /**
     * 记录卡片质量反馈.
     *
     * @param cardId 卡片ID
     * @param rating 评分
     * @param comment 评论
     * @return void
     */
    void recordQualityFeedback(Long cardId, Integer rating, String comment);

    /**
     * 获取用户答题准确率.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 准确率
     */
    int getUserAccuracy(Long userId, Long workspaceId);
}
