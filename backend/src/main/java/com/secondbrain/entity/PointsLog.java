package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 积分变动日志实体.
 *
 * <p>记录每一笔积分收支，用于积分流水查询和审计。</p>
 */
@Getter
@Setter
@TableName("points_log")
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 积分变动（正数为获得，负数为消耗）
     */
    private Integer points;

    /**
     * 积分类型：review / create / checkin / achievement / streak_bonus / makeup_use
     */
    private String type;

    /**
     * 积分描述
     */
    private String description;

    /**
     * 关联ID（如 review_card_id, node_id, achievement_id）
     */
    private Long referenceId;

    private LocalDateTime createTime;
}
