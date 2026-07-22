package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.ChatRequestDTO;
import com.secondbrain.dto.ChatResponseDTO;
import com.secondbrain.entity.ChatMessage;
import com.secondbrain.entity.ChatSession;

/**
 * 聊天会话服务接口.
 * <p>提供AI聊天对话、会话管理等功能</p>
 */
public interface ChatSessionService {

    /**
     * 发送聊天消息.
     *
     * @param request 聊天请求
     * @param userId 用户ID
     * @return 聊天响应
     */
    ChatResponseDTO chat(ChatRequestDTO request, Long userId);

    /**
     * 发送带知识检索的增强聊天消息.
     *
     * @param request 聊天请求
     * @param userId 用户ID
     * @return 聊天响应
     */
    ChatResponseDTO chatWithKnowledge(ChatRequestDTO request, Long userId);

    /**
     * 获取用户的会话列表.
     *
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页会话列表
     */
    Page<ChatSession> getSessionList(Long userId, Integer current, Integer size);

    /**
     * 获取会话的消息列表.
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页消息列表
     */
    Page<ChatMessage> getMessageList(Long sessionId, Long userId, Integer current, Integer size);

    /**
     * 创建新会话.
     *
     * @param userId 用户ID
     * @param title 会话标题
     * @return 会话实体
     */
    ChatSession createSession(Long userId, String title);

    /**
     * 删除指定会话.
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void deleteSession(Long sessionId, Long userId);
}
