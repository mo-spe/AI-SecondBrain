package com.secondbrain.service;

import com.secondbrain.dto.UpdateReviewPreferenceRequest;
import com.secondbrain.vo.ReviewPreferenceVO;

import java.time.LocalDateTime;

/**
 * 个人复习节奏服务。
 *
 * <p>通过用户级偏好计算后续复习时间，不主动改写已有卡片的下次复习时间。</p>
 */
public interface ReviewPreferenceService {

    /**
     * 获取当前用户的有效复习偏好。
     *
     * @param userId 当前用户ID
     * @return 有效偏好；未保存时返回默认节奏
     */
    ReviewPreferenceVO getPreference(Long userId);

    /**
     * 保存当前用户的复习偏好。
     *
     * @param userId 当前用户ID
     * @param request 偏好更新请求
     * @return 保存后的有效偏好
     */
    ReviewPreferenceVO updatePreference(Long userId, UpdateReviewPreferenceRequest request);

    /**
     * 基于用户当前有效节奏计算下一次复习时间。
     *
     * @param userId 用户ID
     * @param lastReviewTime 本次复习完成时间
     * @param reviewCount 本次复习后的累计复习次数
     * @param isCorrect 本次是否答对
     * @return 下次复习时间
     */
    LocalDateTime calculateNextReviewTime(Long userId, LocalDateTime lastReviewTime,
                                          int reviewCount, boolean isCorrect);

    /**
     * 判断用户是否同意通过邮件接收指定提醒。
     *
     * @param userId 用户ID
     * @return 已开启时返回 true
     */
    boolean isReviewEmailEnabled(Long userId);
}
