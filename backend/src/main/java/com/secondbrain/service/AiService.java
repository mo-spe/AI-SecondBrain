package com.secondbrain.service;

import com.secondbrain.dto.KnowledgeDTO;

import java.util.List;

/**
 * AI服务接口.
 * <p>提供知识提取、题目生成、答案生成等AI能力</p>
 */
public interface AiService {

    /**
     * 提取知识.
     *
     * @param content 内容
     * @return 知识列表
     */
    List<KnowledgeDTO> extractKnowledge(String content);
    
    /**
     * 生成题目.
     *
     * @param prompt 提示词
     * @return 题目内容
     */
    String generateQuestion(String prompt);
    
    /**
     * 生成答案.
     *
     * @param prompt 提示词
     * @return 答案内容
     */
    String generateAnswer(String prompt);
    
    /**
     * 提取知识（带API Key）.
     *
     * @param content 内容
     * @param userApiKey 用户API Key
     * @return 知识列表
     */
    List<KnowledgeDTO> extractKnowledge(String content, String userApiKey);
    
    /**
     * 生成题目（带API Key）.
     *
     * @param prompt 提示词
     * @param userApiKey 用户API Key
     * @return 题目内容
     */
    String generateQuestion(String prompt, String userApiKey);
    
    /**
     * 生成答案（带API Key）.
     *
     * @param prompt 提示词
     * @param userApiKey 用户API Key
     * @return 答案内容
     */
    String generateAnswer(String prompt, String userApiKey);

    /**
     * 执行多轮对话.
     *
     * @param messages 消息列表
     * @param userApiKey 用户API Key
     * @return AI的回复内容
     */
    String chat(java.util.List<com.unfbx.chatgpt.entity.chat.Message> messages, String userApiKey);
}
