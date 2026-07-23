package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeRevision;

import java.util.List;

/**
 * 知识节点版本历史服务接口.
 * <p>提供版本快照保存、历史查询和版本回滚功能</p>
 */
public interface KnowledgeRevisionService {

    /**
     * 保存版本快照.
     * 每次更新知识节点后自动调用，记录更新前的内容。
     *
     * @param nodeId    知识节点ID
     * @param userId    编辑用户ID
     * @param title     快照标题
     * @param contentMd 快照内容
     * @param summary   快照摘要
     */
    void saveRevision(Long nodeId, Long userId, String title, String contentMd, String summary);

    /**
     * 获取版本历史列表.
     *
     * @param nodeId 知识节点ID
     * @return 版本历史列表（按版本号降序）
     */
    List<KnowledgeRevision> getRevisionList(Long nodeId);

    /**
     * 获取版本详情.
     *
     * @param revisionId 版本ID
     * @return 版本详情（不存在返回null）
     */
    KnowledgeRevision getRevisionDetail(Long revisionId);

    /**
     * 回滚到指定版本.
     * 将知识节点内容恢复为指定版本的状态，同时创建新的版本记录标记回滚操作。
     * 仅Owner/Admin可执行（调用方需校验权限）。
     *
     * @param nodeId     知识节点ID
     * @param revisionId 目标版本ID
     * @param userId     操作用户ID
     * @return 回滚后知识节点的当前状态
     */
    void rollback(Long nodeId, Long revisionId, Long userId);
}
