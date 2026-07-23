package com.secondbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.BatchChatImportRequest;
import com.secondbrain.dto.ChatCollectRequest;
import com.secondbrain.dto.ChatRecordDTO;
import com.secondbrain.dto.KnowledgeDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话服务接口.
 * <p>提供对话采集、导入、查询及知识提取功能</p>
 */
public interface ChatService {

    /**
     * 采集对话记录.
     *
     * @param request 对话采集请求
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return void
     */
    void collectChat(ChatCollectRequest request, Long userId, Long workspaceId);

    /**
     * 采集对话记录（带用户API Key）.
     *
     * @param request 对话采集请求
     * @param userId 用户ID
     * @param userApiKey 用户API Key
     * @param workspaceId 工作区ID
     * @return void
     */
    void collectChat(ChatCollectRequest request, Long userId, String userApiKey, Long workspaceId);

    /**
     * 批量导入对话记录.
     *
     * @param request 批量导入请求
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 导入数量
     */
    int batchImportChats(BatchChatImportRequest request, Long userId, Long workspaceId);

    /**
     * 批量导入对话记录（带用户API Key）.
     *
     * @param request 批量导入请求
     * @param userId 用户ID
     * @param userApiKey 用户API Key
     * @param workspaceId 工作区ID
     * @return 导入数量
     */
    int batchImportChats(BatchChatImportRequest request, Long userId, String userApiKey, Long workspaceId);

    /**
     * 分页查询对话列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param platform 平台
     * @param keyword 关键词
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 对话记录分页
     */
    Page<ChatRecordDTO> getChatList(Long current, Long size, String platform, String keyword, Long userId, Long workspaceId);

    /**
     * 根据ID查询对话记录.
     *
     * @param id 对话ID
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 对话记录
     */
    ChatRecordDTO getChatById(Long id, Long userId, Long workspaceId);

    /**
     * 从内容中提取知识点.
     *
     * @param content 内容
     * @return 知识点列表
     */
    List<KnowledgeDTO> extractKnowledge(String content);

    /**
     * 从内容中提取知识点（带用户API Key）.
     *
     * @param content 内容
     * @param userApiKey 用户API Key
     * @return 知识点列表
     */
    List<KnowledgeDTO> extractKnowledge(String content, String userApiKey);

    /**
     * 统计用户对话数量.
     *
     * @param userId 用户ID
     * @param workspaceId 工作区ID
     * @return 对话数量
     */
    long countByUserId(Long userId, Long workspaceId);

    /**
     * 统计指定时间范围内的用户对话数量.
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param workspaceId 工作区ID
     * @return 对话数量
     */
    long countByUserIdAndDateRange(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long workspaceId);
}
