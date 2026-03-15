package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.BatchChatImportRequest;
import com.secondbrain.dto.ChatCollectRequest;
import com.secondbrain.dto.ChatRecordDTO;
import com.secondbrain.dto.KnowledgeDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatService {

    void collectChat(ChatCollectRequest request, Long userId);

    void collectChat(ChatCollectRequest request, Long userId, String userApiKey);

    int batchImportChats(BatchChatImportRequest request, Long userId);

    int batchImportChats(BatchChatImportRequest request, Long userId, String userApiKey);

    Page<ChatRecordDTO> getChatList(Long current, Long size, String platform, String keyword, Long userId);

    ChatRecordDTO getChatById(Long id, Long userId);

    List<KnowledgeDTO> extractKnowledge(String content);

    List<KnowledgeDTO> extractKnowledge(String content, String userApiKey);

    long countByUserId(Long userId);

    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
