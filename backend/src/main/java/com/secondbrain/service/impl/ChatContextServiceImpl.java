package com.secondbrain.service.impl;

import com.alibaba.fastjson2.JSON;
import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.service.CacheService;
import com.secondbrain.service.ChatContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ChatContextServiceImpl implements ChatContextService {

    private static final Logger log = LoggerFactory.getLogger(ChatContextServiceImpl.class);

    private static final String CHAT_CONTEXT_PREFIX = "chat:context:";
    private static final String EXTRACTED_KNOWLEDGE_PREFIX = "chat:knowledge:";

    private static final long CACHE_TIMEOUT = 1;

    private final CacheService cacheService;

    public ChatContextServiceImpl(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void cacheChatContext(String chatId, String content) {
        String key = CHAT_CONTEXT_PREFIX + chatId;
        cacheService.set(key, content, CACHE_TIMEOUT, TimeUnit.HOURS);
        log.debug("缓存对话上下文，chatId：{}，长度：{}", chatId, content.length());
    }

    @Override
    public String getChatContext(String chatId) {
        String key = CHAT_CONTEXT_PREFIX + chatId;
        String context = cacheService.get(key, String.class);
        if (context != null) {
            log.debug("命中对话上下文缓存，chatId：{}", chatId);
        }
        return context;
    }

    @Override
    public void cacheExtractedKnowledge(String chatId, List<KnowledgeDTO> knowledgeList) {
        String key = EXTRACTED_KNOWLEDGE_PREFIX + chatId;
        String json = JSON.toJSONString(knowledgeList);
        cacheService.set(key, json, CACHE_TIMEOUT, TimeUnit.HOURS);
        log.debug("缓存提取的知识点，chatId：{}，数量：{}", chatId, knowledgeList.size());
    }

    @Override
    public List<KnowledgeDTO> getCachedKnowledge(String chatId) {
        String key = EXTRACTED_KNOWLEDGE_PREFIX + chatId;
        String json = cacheService.get(key, String.class);
        if (json != null) {
            log.debug("命中知识点缓存，chatId：{}", chatId);
            return JSON.parseArray(json, KnowledgeDTO.class);
        }
        return null;
    }

    @Override
    public void clearChatContext(String chatId) {
        String contextKey = CHAT_CONTEXT_PREFIX + chatId;
        String knowledgeKey = EXTRACTED_KNOWLEDGE_PREFIX + chatId;
        cacheService.delete(contextKey);
        cacheService.delete(knowledgeKey);
        log.debug("清除对话上下文缓存，chatId：{}", chatId);
    }
}
