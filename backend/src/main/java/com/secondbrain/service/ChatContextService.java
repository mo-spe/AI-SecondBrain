package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;

public interface ChatContextService {

    void cacheChatContext(String chatId, String content);

    String getChatContext(String chatId);

    void cacheExtractedKnowledge(String chatId, List<KnowledgeDTO> knowledgeList);

    List<KnowledgeDTO> getCachedKnowledge(String chatId);

    void clearChatContext(String chatId);
}
