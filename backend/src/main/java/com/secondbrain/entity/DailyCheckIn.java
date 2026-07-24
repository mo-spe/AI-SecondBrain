package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到记录实体.
 */
@Getter
@Setter
@TableName("daily_check_in")
public class DailyCheckIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 签到日期
     */
    private LocalDate checkInDate;

    /**
     * 本次签到获得积分
     */
    private Integer pointsEarned;

    /**
     * 连续签到加成倍数
     */
    private java.math.BigDecimal streakBonusMultiplier;

    /**
     * 是否为补签（0-正常签到，1-补签）
     */
    private Integer isMakeup;

    private LocalDateTime createTime;
}
