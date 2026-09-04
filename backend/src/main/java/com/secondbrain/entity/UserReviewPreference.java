package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户复习偏好实体。
 *
 * <p>偏好独立于复习卡片保存，因此修改节奏不会重排已经生成的复习计划。</p>
 */
@Getter
@Setter
@TableName("user_review_preference")
public class UserReviewPreference {

    /**
     * 用户ID，同时作为偏好记录主键。
     */
    @TableId
    private Long userId;

    /**
     * 自定义复习间隔天数的 JSON 数组。
     */
    private String intervalDaysJson;

    /**
     * 是否允许在指定提醒到期时发送邮件，0-否，1-是。
     */
    private Integer reviewEmailEnabled;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
