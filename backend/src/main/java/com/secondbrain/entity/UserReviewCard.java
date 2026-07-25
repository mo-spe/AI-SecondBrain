package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户复习卡片实体（个人副本）.
 * <p>每个用户独立持有，追踪个人的复习进度。通过 pool_id 关联题目池模板</p>
 */
@Getter
@Setter
@TableName("user_review_card")
public class UserReviewCard {

    /**
     * 个人副本ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联池子题目ID（NULL 表示无池子模板的个人卡片）
     */
    private Long poolId;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 工作区ID
     */
    private Long workspaceId;

    /**
     * 复习次数
     */
    private Integer reviewCount;

    /**
     * 正确次数
     */
    private Integer correctCount;

    /**
     * 错误次数
     */
    private Integer incorrectCount;

    /**
     * 掌握程度 0-5
     */
    private Integer masteryLevel;

    /**
     * 记忆强度 0-1
     */
    private Double memoryStrength;

    /**
     * 上次复习时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 状态（0=待复习，1=已掌握）
     */
    private Integer status;

    /**
     * 是否归档（重新加入后旧副本标1，保留历史记录）
     */
    private Integer isArchived;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
