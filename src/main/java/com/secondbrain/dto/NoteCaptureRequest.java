package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "笔记捕捉请求")
public class NoteCaptureRequest {
    
    @Schema(description = "笔记标题")
    private String title;
    
    @Schema(description = "笔记内容")
    private String content;
    
    @Schema(description = "用户ID")
    private Long userId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
