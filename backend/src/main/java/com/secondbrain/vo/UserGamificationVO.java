package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户游戏化概览 VO.
 */
@Getter
@Setter
public class UserGamificationVO {

    private Long totalPoints;
    private Long currentPoints;
    private Integer level;
    private Long experience;
    private Long experienceToNextLevel;
    private Double levelProgressPercent;
    private Integer currentStreak;
    private Integer maxStreak;
    private LocalDate lastCheckInDate;
    private Boolean checkedInToday;
    private Integer makeupCardsRemaining;
    private Integer totalReviewCount;
    private Integer totalCorrectCount;
    private Integer totalNodeCount;
    private Double accuracyRate;

    /**
     * 下一个待解锁成就
     */
    private AchievementVO nextAchievement;

    /**
     * 本次操作新解锁的成就列表（签到/补签时返回）
     */
    private List<AchievementVO> newAchievements;

    /**
     * 本次操作获得的积分（签到/补签时返回）
     */
    private Integer pointsEarned;
}
