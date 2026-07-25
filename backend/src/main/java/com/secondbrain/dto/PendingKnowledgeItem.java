package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 待确认知识点的单个条目 DTO.
 */
@Getter
@Setter
public class PendingKnowledgeItem {

    /**
     * 待确认记录的 ID（用于更新状态）
     */
    private Long pendingId;

    /**
     * 知识点标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 详细内容
     */
    private String content;
}
