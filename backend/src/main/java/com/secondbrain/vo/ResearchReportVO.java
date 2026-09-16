package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究报告视图对象.
 *
 * @author AI
 */
@Getter
@Setter
public class ResearchReportVO {

    /**
     * 报告ID
     */
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 报告标题
     */
    private String title;

    /**
     * 执行摘要
     */
    private String summary;

    /**
     * 完整报告（Markdown）
     */
    private String contentMd;

    /**
     * 关键发现（JSON）
     */
    private String keyFindings;

    /**
     * 知识缺口（JSON）
     */
    private String knowledgeGaps;

    /**
     * 来源数量
     */
    private Integer sourceCount;

    /**
     * 结论数量
     */
    private Integer conclusionCount;

    /**
     * 总耗时（毫秒）
     */
    private Long durationTotalMs;

    /**
     * 生成者标识
     */
    private String generatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
