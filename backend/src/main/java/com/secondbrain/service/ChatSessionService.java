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
     * AI对话.
     *
     * @param request 对话请求
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 对话响应
     */
    ChatResponseDTO chat(ChatRequestDTO request, Long userId, Long workspaceId);

    /**
     * 基于知识库的AI对话.
     *
     * @param request 对话请求
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 对话响应
     */
    ChatResponseDTO chatWithKnowledge(ChatRequestDTO request, Long userId, Long workspaceId);

    /**
     * 分页查询会话列表.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param current 当前页
     * @param size 每页大小
     * @return 会话分页
     */
    Page<ChatSession> getSessionList(Long userId, Long workspaceId, Integer current, Integer size);

    /**
     * 分页查询会话消息列表.
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param current 当前页
     * @param size 每页大小
     * @return 消息分页
     */
    Page<ChatMessage> getMessageList(Long sessionId, Long userId, Integer current, Integer size);

    /**
     * 创建会话.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @param title 会话标题
     * @return 会话
     */
    ChatSession createSession(Long userId, Long workspaceId, String title);

    /**
     * 删除会话.
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void deleteSession(Long sessionId, Long userId, Long workspaceId);
}
