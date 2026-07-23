package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 编辑锁实体.
 * <p>同一知识节点只有一条锁记录，通过数据库唯一约束+原子操作防止竞态条件</p>
 */
@Getter
@Setter
@TableName("editing_lock")
public class EditingLock {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识节点ID（唯一约束）
     */
    private Long nodeId;

    /**
     * 持有锁的用户ID
     */
    private Long userId;

    /**
     * 锁获取时间
     */
    private LocalDateTime acquiredAt;

    /**
     * 锁过期时间（默认+15分钟，支持续期）
     */
    private LocalDateTime expiresAt;
}
