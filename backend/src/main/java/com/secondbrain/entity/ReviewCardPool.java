package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 复习卡片题目池实体.
 * <p>工作区级的卡片模板，同一知识点在工作区内只生成一次，成员通过"加入复习"获取个人副本</p>
 */
@Getter
@Setter
@TableName("review_card_pool")
public class ReviewCardPool {

    /**
     * 池子题目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long nodeId;

    /**
     * 所属工作区ID
     */
    private Long workspaceId;

    /**
     * 题目内容
     */
    private String question;

    /**
     * 正确答案
     */
    private String answer;

    /**
     * 题型（choice/fill/essay/judge）
     */
    private String cardType;

    /**
     * 初始难度 1-5
     */
    private Integer difficulty;

    /**
     * 生成方式（auto/manual）
     */
    private String generationType;

    /**
     * 创建者用户ID
     */
    private Long createUserId;

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

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
