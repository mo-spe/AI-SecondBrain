package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 成就 VO（含用户进度信息）.
 */
@Getter
@Setter
public class AchievementVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String tier;
    private Integer triggerValue;
    private Integer pointsReward;
    private Integer makeupCardReward;

    /**
     * 当前用户进度值
     */
    private Integer currentValue;

    /**
     * 进度百分比（0-100）
     */
    private Integer progressPercent;

    /**
     * 是否已解锁
     */
    private Boolean unlocked;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockedAt;
}
