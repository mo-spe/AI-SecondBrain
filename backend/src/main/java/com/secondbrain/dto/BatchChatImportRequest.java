package com.secondbrain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "批量对话导入请求")
public class BatchChatImportRequest {

    @Schema(description = "对话列表")
    private List<ChatItem> chats;

    public List<ChatItem> getChats() {
        return chats;
    }

    public void setChats(List<ChatItem> chats) {
        this.chats = chats;
    }

    @Schema(description = "对话项")
    public static class ChatItem {
        @Schema(description = "对话内容")
        private String content;

        @Schema(description = "平台")
        private String platform;

        @Schema(description = "来源URL")
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
}
