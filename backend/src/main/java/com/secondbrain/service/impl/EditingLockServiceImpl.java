package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.secondbrain.entity.EditingLock;
import com.secondbrain.mapper.EditingLockMapper;
import com.secondbrain.service.EditingLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 编辑锁服务实现.
 * <p>使用数据库唯一约束实现编辑锁，通过mybatis-plus操作editing_lock表</p>
 */
@Service
public class EditingLockServiceImpl implements EditingLockService {

    private static final Logger log = LoggerFactory.getLogger(EditingLockServiceImpl.class);

    /** 锁超时时间（分钟） */
    private static final int LOCK_TIMEOUT_MINUTES = 15;

    private final EditingLockMapper editingLockMapper;

    public EditingLockServiceImpl(EditingLockMapper editingLockMapper) {
        this.editingLockMapper = editingLockMapper;
    }

    /**
     * 获取编辑锁.
     * 先查询当前锁状态：若锁不存在或已过期则尝试获取；若自己已持有则续期；若被他人持有则返回null。
     */
    @Override
    public EditingLock acquireLock(Long nodeId, Long userId) {
        EditingLock existingLock = getLockStatus(nodeId);

        if (existingLock == null) {
            return createLock(nodeId, userId);
        }

        if (existingLock.getUserId().equals(userId)) {
            renewLock(nodeId, userId);
            return getLockStatus(nodeId);
        }

        log.info("lock_conflict nodeId={} requestedBy={} heldBy={}", nodeId, userId, existingLock.getUserId());
        return null;
    }

    /**
     * 释放编辑锁.
     * 仅锁持有者可释放。
     */
    @Override
    public void releaseLock(Long nodeId, Long userId) {
        LambdaQueryWrapper<EditingLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EditingLock::getNodeId, nodeId)
                .eq(EditingLock::getUserId, userId);
        int deleted = editingLockMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("lock_released nodeId={} userId={}", nodeId, userId);
        }
    }

    /**
     * 查询锁状态.
     * 若锁已过期则自动清理并返回null。
     */
    @Override
    public EditingLock getLockStatus(Long nodeId) {
        LambdaQueryWrapper<EditingLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EditingLock::getNodeId, nodeId);
        EditingLock lock = editingLockMapper.selectOne(wrapper);

        if (lock != null && lock.getExpiresAt().isBefore(LocalDateTime.now())) {
            editingLockMapper.deleteById(lock.getId());
            return null;
        }

        return lock;
    }

    /**
     * 续期编辑锁.
     * 仅锁持有者（需匹配userId）可续期，将过期时间延长15分钟。
     */
    @Override
    public void renewLock(Long nodeId, Long userId) {
        LambdaUpdateWrapper<EditingLock> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EditingLock::getNodeId, nodeId)
                .eq(EditingLock::getUserId, userId)
                .set(EditingLock::getExpiresAt, LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES));
        editingLockMapper.update(null, wrapper);
    }

    /**
     * 创建新的编辑锁.
     * 使用数据库唯一约束保证原子性——若node_id已存在则插入失败。
     */
    private EditingLock createLock(Long nodeId, Long userId) {
        EditingLock lock = new EditingLock();
        lock.setNodeId(nodeId);
        lock.setUserId(userId);
        lock.setAcquiredAt(LocalDateTime.now());
        lock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES));

        try {
            editingLockMapper.insert(lock);
            log.info("lock_acquired nodeId={} userId={} expiresAt={}", nodeId, userId, lock.getExpiresAt());
            return lock;
        } catch (Exception e) {
            log.info("lock_insert_conflict nodeId={} userId={}", nodeId, userId);
            return null;
        }
    }

    /**
     * 定时清理过期锁.
     * 每分钟执行一次，删除所有已过期的锁记录。
     */
    @Scheduled(fixedRate = 60000)
    public void cleanExpiredLocks() {
        LambdaQueryWrapper<EditingLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(EditingLock::getExpiresAt, LocalDateTime.now());
        long deleted = editingLockMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("cleaned_expired_locks count={}", deleted);
        }
    }
}
