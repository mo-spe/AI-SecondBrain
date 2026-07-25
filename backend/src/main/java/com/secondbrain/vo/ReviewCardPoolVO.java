package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 题目池列表 VO.
 * <p>包含池子题目基本信息 + 社区统计 + 当前用户的加入状态</p>
 */
@Getter
@Setter
public class ReviewCardPoolVO {

    /**
     * 池子题目ID
     */
    private Long id;

    /**
     * 关联知识点ID
     */
    private Long nodeId;

    /**
     * 知识点标题
     */
    private String nodeTitle;

    /**
     * 题目摘要（前100字）
     */
    private String questionPreview;

    /**
     * 题型
     */
    private String cardType;

    /**
     * 难度 1-5
     */
    private Integer difficulty;

    /**
     * 生成方式
     */
    private String generationType;

    /**
     * 创建者用户ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 参与复习的成员数
     */
    private Integer memberCount;

    /**
     * 社区标签颜色代码（green/yellow/red）
     */
    private String communityLabel;

    /**
     * 社区标签中文描述
     */
    private String communityText;

    /**
     * 当前用户是否已加入复习
     */
    private Boolean isJoined;

    /**
     * 当前用户的个人副本ID（已加入时有值）
     */
    private Long userCardId;
}
