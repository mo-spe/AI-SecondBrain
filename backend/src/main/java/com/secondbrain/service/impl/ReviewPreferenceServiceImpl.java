package com.secondbrain.service.impl;

import com.secondbrain.dto.UpdateReviewPreferenceRequest;
import com.secondbrain.entity.UserReviewPreference;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.UserReviewPreferenceMapper;
import com.secondbrain.service.ReviewPreferenceService;
import com.secondbrain.vo.ReviewPreferenceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 个人复习节奏服务实现。
 *
 * <p>默认值保持既有艾宾浩斯间隔，只有用户之后提交复习答案时才读取新偏好，
 * 以避免设置变更意外改写正在进行的学习计划。</p>
 */
@Service
public class ReviewPreferenceServiceImpl implements ReviewPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPreferenceServiceImpl.class);

    private static final List<Integer> DEFAULT_INTERVAL_DAYS = List.of(1, 7, 14, 30, 60);
    private static final int MIN_INTERVAL_COUNT = 3;
    private static final int MAX_INTERVAL_COUNT = 8;
    private static final int MAX_INTERVAL_DAYS = 3650;
    private static final long MINUTES_PER_DAY = 24L * 60L;

    private final UserReviewPreferenceMapper preferenceMapper;

    public ReviewPreferenceServiceImpl(UserReviewPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    /**
     * 获取当前用户的有效复习偏好。
     *
     * @param userId 当前用户ID
     * @return 有效偏好；没有持久化配置时返回默认值
     */
    @Override
    public ReviewPreferenceVO getPreference(Long userId) {
        UserReviewPreference preference = preferenceMapper.selectById(userId);
        return toView(preference, preference == null);
    }

    /**
     * 保存个人复习偏好。
     *
     * @param userId 当前用户ID
     * @param request 偏好更新请求
     * @return 保存后的偏好
     */
    @Override
    @Transactional
    public ReviewPreferenceVO updatePreference(Long userId, UpdateReviewPreferenceRequest request) {
        List<Integer> intervalDays = validateIntervalDays(request.getIntervalDays());
        UserReviewPreference preference = preferenceMapper.selectById(userId);
        boolean isNew = preference == null;

        if (isNew) {
            preference = new UserReviewPreference();
            preference.setUserId(userId);
            preference.setReviewEmailEnabled(Boolean.TRUE.equals(request.getReviewEmailEnabled()) ? 1 : 0);
        } else if (request.getReviewEmailEnabled() != null) {
            preference.setReviewEmailEnabled(request.getReviewEmailEnabled() ? 1 : 0);
        }

        preference.setIntervalDaysJson(toJson(intervalDays));
        if (isNew) {
            preferenceMapper.insert(preference);
        } else {
            preferenceMapper.updateById(preference);
        }

        log.info("review_preference_updated userId={} intervalDays={} emailEnabled={}",
                userId, intervalDays, preference.getReviewEmailEnabled());
        return toView(preference, false);
    }

    /**
     * 基于当前偏好计算下次复习时间。
     *
     * @param userId 用户ID
     * @param lastReviewTime 本次复习完成时间
     * @param reviewCount 本次复习后的累计复习次数
     * @param isCorrect 本次是否答对
     * @return 下次复习时间
     */
    @Override
    public LocalDateTime calculateNextReviewTime(Long userId, LocalDateTime lastReviewTime,
                                                  int reviewCount, boolean isCorrect) {
        List<Integer> intervalDays = getEffectiveIntervalDays(userId);
        int index = Math.min(Math.max(reviewCount, 0), intervalDays.size() - 1);
        long selectedIntervalMinutes = intervalDays.get(index) * MINUTES_PER_DAY;
        long firstIntervalMinutes = intervalDays.get(0) * MINUTES_PER_DAY;

        // 错误回答仍保留既有“缩短间隔但不短于首段”的策略，避免一次失误把复习强制拉回起点。
        long effectiveIntervalMinutes = isCorrect
                ? selectedIntervalMinutes
                : Math.max(firstIntervalMinutes, selectedIntervalMinutes / 2);
        LocalDateTime baseTime = lastReviewTime == null ? LocalDateTime.now() : lastReviewTime;
        return baseTime.plusMinutes(effectiveIntervalMinutes);
    }

    /**
     * 判断是否开启指定提醒邮件。
     *
     * @param userId 用户ID
     * @return 已开启时返回 true
     */
    @Override
    public boolean isReviewEmailEnabled(Long userId) {
        UserReviewPreference preference = preferenceMapper.selectById(userId);
        return preference != null && Integer.valueOf(1).equals(preference.getReviewEmailEnabled());
    }

    private List<Integer> getEffectiveIntervalDays(Long userId) {
        UserReviewPreference preference = preferenceMapper.selectById(userId);
        if (preference == null) {
            return DEFAULT_INTERVAL_DAYS;
        }
        return parseStoredIntervals(preference.getIntervalDaysJson(), userId);
    }

    private List<Integer> validateIntervalDays(List<Integer> intervalDays) {
        if (intervalDays == null || intervalDays.size() < MIN_INTERVAL_COUNT || intervalDays.size() > MAX_INTERVAL_COUNT) {
            throw new BusinessException(400, "复习间隔需设置为3至8个阶段");
        }

        int previous = 0;
        for (Integer intervalDay : intervalDays) {
            if (intervalDay == null || intervalDay <= 0 || intervalDay > MAX_INTERVAL_DAYS) {
                throw new BusinessException(400, "每个复习间隔需为1至3650天");
            }
            if (intervalDay <= previous) {
                throw new BusinessException(400, "复习间隔必须严格递增");
            }
            previous = intervalDay;
        }
        return List.copyOf(intervalDays);
    }

    private List<Integer> parseStoredIntervals(String rawIntervals, Long userId) {
        if (rawIntervals == null || rawIntervals.isBlank()) {
            return DEFAULT_INTERVAL_DAYS;
        }
        try {
            String value = rawIntervals.trim();
            if (!value.startsWith("[") || !value.endsWith("]")) {
                throw new IllegalArgumentException("不是 JSON 数组");
            }
            String content = value.substring(1, value.length() - 1).trim();
            List<Integer> intervals = content.isEmpty()
                    ? List.of()
                    : Arrays.stream(content.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            return validateIntervalDays(intervals);
        } catch (RuntimeException exception) {
            // 配置损坏时宁可回退到稳定的默认节奏，也不能让单次答题无法完成。
            log.warn("review_preference_invalid_fallback_default userId={}", userId, exception);
            return DEFAULT_INTERVAL_DAYS;
        }
    }

    private String toJson(List<Integer> intervalDays) {
        return intervalDays.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private ReviewPreferenceVO toView(UserReviewPreference preference, boolean usingDefaultIntervals) {
        ReviewPreferenceVO view = new ReviewPreferenceVO();
        view.setIntervalDays(new ArrayList<>(preference == null
                ? DEFAULT_INTERVAL_DAYS
                : parseStoredIntervals(preference.getIntervalDaysJson(), preference.getUserId())));
        view.setReviewEmailEnabled(preference != null && Integer.valueOf(1).equals(preference.getReviewEmailEnabled()));
        view.setUsingDefaultIntervals(usingDefaultIntervals);
        return view;
    }
}
