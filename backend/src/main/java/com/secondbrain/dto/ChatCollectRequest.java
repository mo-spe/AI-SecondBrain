package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "对话采集请求")
public class ChatCollectRequest {

    @NotBlank(message = "对话内容不能为空")
    @Schema(description = "对话内容")
    private String content;

    @Schema(description = "来源平台")
    private String platform;

    @Schema(description = "原始链接")
    private String sourceUrl;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
