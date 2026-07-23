package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 举报记录视图对象.
 */
@Getter
@Setter
public class SquareReportVO {

    /**
     * 举报ID
     */
    private Long id;

    /**
     * 被举报帖子ID
     */
    private Long postId;

    /**
     * 被举报帖子标题（关联知识节点）
     */
    private String postTitle;

    /**
     * 举报人用户ID
     */
    private Long reporterId;

    /**
     * 举报人用户名
     */
    private String reporterName;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 处理状态：pending / ignored / removed
     */
    private String status;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 举报时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
}
