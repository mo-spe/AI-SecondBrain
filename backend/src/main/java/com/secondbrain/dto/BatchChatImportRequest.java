package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量对话导入请求DTO.
 */
@Getter
@Setter
public class BatchChatImportRequest {

    /**
     * 对话列表
     */
    private List<ChatItem> chats;

    /**
     * 对话项内部类.
     */
    @Getter
    @Setter
    public static class ChatItem {

        /**
         * 对话内容
         */
        private String content;

        /**
         * 来源平台
         */
        private String platform;

        /**
         * 来源URL
         */
        private String sourceUrl;
    }
}
