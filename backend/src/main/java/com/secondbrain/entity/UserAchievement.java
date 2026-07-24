package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户成就记录实体.
 *
 * <p>记录用户解锁了哪些成就及解锁时间。</p>
 */
@Getter
@Setter
@TableName("user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成就ID
     */
    private Long achievementId;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockedAt;

    /**
     * 是否已通知前端（0-未通知，1-已通知）
     */
    private Integer notified;

    private LocalDateTime createTime;
}
