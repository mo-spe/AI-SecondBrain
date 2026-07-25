package com.secondbrain.service;

import com.secondbrain.dto.BatchConfirmRequest;
import com.secondbrain.entity.PendingKnowledge;

import java.util.List;

/**
 * 待确认知识点服务接口.
 * <p>AI 提取的知识点先存入 pending_knowledge 表，经用户确认后才迁移到 knowledge_node</p>
 */
public interface PendingKnowledgeService {

    /**
     * 获取待确认知识点列表.
     *
     * @param userId      用户ID
     * @param workspaceId 工作区ID
     * @return 待确认知识点列表
     */
    List<PendingKnowledge> listPending(Long userId, Long workspaceId);

    /**
     * 批量确认入库.
     * <p>将选中的知识点迁移到 knowledge_node，可选是否生成复习卡片</p>
     *
     * @param userId  用户ID
     * @param request 批量确认请求
     */
    void confirmBatch(Long userId, BatchConfirmRequest request);

    /**
     * 丢弃单条待确认知识点.
     *
     * @param userId    用户ID
     * @param pendingId 待确认记录ID
     */
    void discardPending(Long userId, Long pendingId);

    /**
     * 手动新增待确认知识点.
     *
     * @param userId 用户ID
     * @param item   知识点内容
     * @return 新增的待确认记录
     */
    PendingKnowledge addPending(Long userId, PendingKnowledge item);
}
