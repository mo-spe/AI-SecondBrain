package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 成就定义实体.
 *
 * <p>存储成就目录（种子数据），通过 trigger_type + trigger_value 定义解锁条件。</p>
 */
@Getter
@Setter
@TableName("achievement")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 成就代码（唯一标识，如 first_review）
     */
    private String code;

    /**
     * 成就名称
     */
    private String name;

    /**
     * 成就描述
     */
    private String description;

    /**
     * 成就图标（Element Plus icon name）
     */
    private String icon;

    /**
     * 成就分类：review / knowledge / streak / accuracy / mastery
     */
    private String category;

    /**
     * 等级：bronze / silver / gold / platinum
     */
    private String tier;

    /**
     * 触发类型
     */
    private String triggerType;

    /**
     * 触发阈值
     */
    private Integer triggerValue;

    /**
     * 解锁奖励积分
     */
    private Integer pointsReward;

    /**
     * 解锁奖励补签卡数
     */
    private Integer makeupCardReward;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
