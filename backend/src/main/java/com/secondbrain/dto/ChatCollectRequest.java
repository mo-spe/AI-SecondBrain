package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 对话采集请求DTO.
 */
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
}
