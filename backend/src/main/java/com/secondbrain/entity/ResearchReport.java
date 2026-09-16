package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 研究报告实体.
 *
 * <p>存储 Synthesizer Agent 生成的最终研究报告，支持版本管理。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_report")
public class ResearchReport {

    /**
     * 报告ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 报告版本号
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
     * 研究问题列表（JSON）
     */
    private String researchQuestions;

    /**
     * 关键发现（JSON）
     */
    private String keyFindings;

    /**
     * 知识缺口（JSON）
     */
    private String knowledgeGaps;

    /**
     * 新写入的知识节点ID列表（JSON）
     */
    private String newKnowledgeIds;

    /**
     * 新写入的关系ID列表（JSON）
     */
    private String newRelationIds;

    /**
     * 来源数量
     */
    private Integer sourceCount;

    /**
     * 结论数量
     */
    private Integer conclusionCount;

    /**
     * Token 总消耗（JSON: {"prompt": N, "completion": N}）
     */
    private String tokenUsageTotal;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
