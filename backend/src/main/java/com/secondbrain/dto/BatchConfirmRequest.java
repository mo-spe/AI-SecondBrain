package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量确认待确认知识点的请求 DTO.
 */
@Getter
@Setter
public class BatchConfirmRequest {

    /**
     * 待确认的知识点列表
     */
    private List<PendingKnowledgeItem> items;

    /**
     * 确认入库后是否同时生成复习卡片
     */
    private Boolean generateCards;
}
