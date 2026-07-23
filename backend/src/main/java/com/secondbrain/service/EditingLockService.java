package com.secondbrain.service;

import com.secondbrain.entity.EditingLock;

/**
 * 编辑锁服务接口.
 * <p>提供编辑锁的获取、释放、查询和续期功能，防止多人同时编辑导致内容覆盖</p>
 */
public interface EditingLockService {

    /**
     * 获取编辑锁.
     * 若锁未被持有或已过期则获取成功；若被他人持有则返回null；若自己已持有则续期。
     *
     * @param nodeId 知识节点ID
     * @param userId 用户ID
     * @return 锁记录（获取失败返回null）
     */
    EditingLock acquireLock(Long nodeId, Long userId);

    /**
     * 释放编辑锁.
     * 仅锁持有者可释放。
     *
     * @param nodeId 知识节点ID
     * @param userId 用户ID
     */
    void releaseLock(Long nodeId, Long userId);

    /**
     * 查询锁状态.
     *
     * @param nodeId 知识节点ID
     * @return 锁记录（不存在或已过期返回null）
     */
    EditingLock getLockStatus(Long nodeId);

    /**
     * 续期编辑锁.
     * 仅锁持有者可续期，将过期时间延长15分钟。
     *
     * @param nodeId 知识节点ID
     * @param userId 用户ID
     */
    void renewLock(Long nodeId, Long userId);
}
