package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;

/**
 * 对话上下文服务接口.
 * <p>提供对话上下文的缓存和管理功能</p>
 */
public interface ChatContextService {

    /**
     * 缓存对话上下文.
     *
     * @param chatId 对话ID
     * @param content 对话内容
     */
    void cacheChatContext(String chatId, String content);

    /**
     * 获取对话上下文.
     *
     * @param chatId 对话ID
     * @return 对话内容
     */
    String getChatContext(String chatId);

    /**
     * 缓存提取的知识.
     *
     * @param chatId 对话ID
     * @param knowledgeList 知识列表
     */
    void cacheExtractedKnowledge(String chatId, List<KnowledgeDTO> knowledgeList);

    /**
     * 获取缓存的知识.
     *
     * @param chatId 对话ID
     * @return 知识列表
     */
    List<KnowledgeDTO> getCachedKnowledge(String chatId);

    /**
     * 清除对话上下文.
     *
     * @param chatId 对话ID
     */
    void clearChatContext(String chatId);
}
