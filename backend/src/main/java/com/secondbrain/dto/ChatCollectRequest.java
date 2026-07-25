package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 对话采集请求DTO. <p>用于对话采集的请求参数封装</p> */
@Getter
@Setter
public class ChatCollectRequest {

    /**
     * 对话内容
     */
    @NotBlank(message = "对话内容不能为空")
    private String content;

    /**
     * 来源平台
     */
    private String platform;

    /**
     * 原始链接
     */
    private String sourceUrl;

    /**
     * 目标工作区ID（NULL=个人空间）
     */
    private Long workspaceId;

    /**
     * 是否提取知识点，默认true
     */
    private Boolean extractKnowledge;

    /**
     * 是否在确认入库时生成复习卡片，默认false
     */
    private Boolean generateCards;
}
