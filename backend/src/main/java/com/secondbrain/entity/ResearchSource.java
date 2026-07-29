package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 研究来源实体.
 *
 * <p>存储 Research Agent 从外部获取的信息源，按可靠性分级并支持全文检索。
 * 来源由 Agent 在执行过程中自动创建，用户仅可查看和删除。</p>
 *
 * @author AI
 */
@Getter
@Setter
@TableName("research_source")
public class ResearchSource {

    /**
     * 来源ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 关联任务ID（可选）
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
     * 来源类型：web_search/official_doc/paper/github/article/internal
     */
    private String sourceType;

    /**
     * 内容摘要
     */
    private String snippet;

    /**
     * 完整抓取内容
     */
    private String fullContent;

    /**
     * 与研究的关联度 0-1
     */
    private BigDecimal relevanceScore;

    /**
     * 可靠性：high/medium/low/unverified
     */
    private String reliability;

    /**
     * 内容SHA256哈希（去重用）
     */
    private String contentHash;

    /**
     * 抓取状态：success/failed/timeout/skipped
     */
    private String fetchStatus;

    /**
     * 内容抓取时间
     */
    private LocalDateTime fetchedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
