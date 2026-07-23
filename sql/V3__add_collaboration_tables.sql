-- 协作共享知识库 MVP — 数据库迁移
-- 新增：编辑锁表、版本历史表、分享链接表（预留V1.1）
-- 修改：workspace_member 增加 status 字段支持邀请确认流程

-- 编辑锁表（防止多人同时编辑同一知识节点导致内容覆盖丢失）
CREATE TABLE IF NOT EXISTS editing_lock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL UNIQUE COMMENT '知识节点ID（唯一约束：同一节点只有一条锁记录）',
    user_id BIGINT NOT NULL COMMENT '持有锁的用户ID',
    acquired_at DATETIME NOT NULL COMMENT '锁获取时间',
    expires_at DATETIME NOT NULL COMMENT '锁过期时间（默认+15分钟）',
    INDEX idx_node_id (node_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编辑锁';

-- 版本历史表（每次更新自动保存快照）
CREATE TABLE IF NOT EXISTS knowledge_revision (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    user_id BIGINT NOT NULL COMMENT '编辑用户ID',
    title VARCHAR(500) COMMENT '快照标题',
    content_md LONGTEXT COMMENT '快照内容',
    summary TEXT COMMENT '快照摘要',
    revision_num INT NOT NULL COMMENT '版本号（每个节点独立自增）',
    change_summary VARCHAR(500) COMMENT '变更说明',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识节点版本历史';

-- 分享链接表（预留V1.1对外分享功能）
CREATE TABLE IF NOT EXISTS share_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    owner_id BIGINT NOT NULL COMMENT '分享者用户ID',
    token VARCHAR(64) NOT NULL UNIQUE COMMENT '随机分享令牌（SHA-256生成，64字符，不可猜测）',
    expire_type VARCHAR(20) NOT NULL DEFAULT 'permanent' COMMENT '有效期类型：permanent/7d/24h',
    expires_at DATETIME COMMENT '具体过期时间（permanent时为null）',
    access_count INT DEFAULT 0 COMMENT '累计访问次数',
    is_revoked TINYINT DEFAULT 0 COMMENT '是否已撤销（0-未撤销，1-已撤销）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_node_id (node_id),
    INDEX idx_owner_id (owner_id),
    INDEX idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识节点分享链接';

-- workspace_member 增加 status 字段（pending表示待确认，accepted表示已确认）
ALTER TABLE workspace_member ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'accepted' COMMENT '成员状态：pending-待确认，accepted-已确认';
ALTER TABLE workspace_member ADD INDEX IF NOT EXISTS idx_status (status);

-- 将已有的成员记录标记为accepted（历史数据直接视为已确认）
UPDATE workspace_member SET status = 'accepted' WHERE status IS NULL;
