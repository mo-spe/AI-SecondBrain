package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户游戏化状态实体.
 *
 * <p>独立于 user 表，存储积分、等级、连续打卡、补签卡等游戏化数据。
 * 设计为独立表以保持 user 表的精简，游戏化模块可独立扩展。</p>
 */
@Getter
@Setter
@TableName("user_gamification")
public class UserGamification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（唯一）
     */
    private Long userId;

    /**
     * 累计总积分
     */
    private Long totalPoints;

    /**
     * 当前可用积分
     */
    private Long currentPoints;

    /**
     * 用户等级（1-50）
     */
    private Integer level;

    /**
     * 当前等级经验值
     */
    private Long experience;

    /**
     * 升级所需经验值
     */
    private Long experienceToNextLevel;

    /**
     * 当前连续签到天数
     */
    private Integer currentStreak;

    /**
     * 历史最长连续签到天数
     */
    private Integer maxStreak;

    /**
     * 最后签到日期
     */
    private LocalDate lastCheckInDate;

    /**
     * 最后复习日期
     */
    private LocalDate lastReviewDate;

    /**
     * 剩余补签卡数量
     */
    private Integer makeupCardsRemaining;

    /**
     * 本月已使用补签卡数
     */
    private Integer makeupCardsUsedThisMonth;

    /**
     * 累计复习次数
     */
    private Integer totalReviewCount;

    /**
     * 累计正确次数
     */
    private Integer totalCorrectCount;

    /**
     * 累计创建知识节点数
     */
    private Integer totalNodeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
