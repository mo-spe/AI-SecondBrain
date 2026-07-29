package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 研究来源视图对象.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchSourceVO {

    /**
     * 来源ID
     */
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 来源标题
     */
    private String title;

    /**
     * 来源URL
     */
    private String url;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源类型中文描述
     */
    private String sourceTypeLabel;

    /**
     * 内容摘要
     */
    private String snippet;

    /**
     * 完整内容（仅详情接口返回）
     */
    private String fullContent;

    /**
     * 关联度 0-1
     */
    private BigDecimal relevanceScore;

    /**
     * 可靠性
     */
    private String reliability;

    /**
     * 可靠性中文描述
     */
    private String reliabilityLabel;

    /**
     * 抓取状态
     */
    private String fetchStatus;

    /**
     * 抓取状态中文描述
     */
    private String fetchStatusLabel;

    /**
     * 内容抓取时间
     */
    private LocalDateTime fetchedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
