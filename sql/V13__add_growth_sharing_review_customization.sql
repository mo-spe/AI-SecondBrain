-- 学习成长、合集发布与复习自定义 V13
-- 本迁移只新增关联与个人设置表，不改动已有帖子和复习卡表。

CREATE TABLE IF NOT EXISTS square_post_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '广场帖子ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    `position` INT NOT NULL COMMENT '节点在合集中的排序位置，从0开始',
    created_at DATETIME NOT NULL COMMENT '关联创建时间',
    UNIQUE KEY uk_square_post_node (post_id, node_id),
    INDEX idx_square_post_node_post_position (post_id, `position`),
    INDEX idx_square_post_node_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场帖子知识节点关联';

-- 将既有单节点帖子补齐为位置为0的合集，保证新查询和关键词搜索能覆盖历史数据。
INSERT IGNORE INTO square_post_node (post_id, node_id, `position`, created_at)
SELECT id, node_id, 0, created_at
FROM square_post
WHERE node_id IS NOT NULL;

-- 用户偏好独立于复习卡保存，避免修改节奏时意外重排既有复习计划。
CREATE TABLE IF NOT EXISTS user_review_preference (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID，同时作为偏好记录主键',
    interval_days_json VARCHAR(255) NULL COMMENT '自定义复习间隔天数的JSON数组',
    review_email_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启复习提醒邮件：0否，1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户复习偏好';

-- 同一用户和知识点仅保留一个提醒；唯一约束让更新、取消与定时任务具备幂等基础。
CREATE TABLE IF NOT EXISTS knowledge_review_reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '提醒ID',
    user_id BIGINT NOT NULL COMMENT '接收提醒的用户ID',
    node_id BIGINT NOT NULL COMMENT '关联知识点ID',
    scheduled_at DATETIME NOT NULL COMMENT '用户指定的提醒时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '状态：SCHEDULED或SENT',
    sent_at DATETIME NULL COMMENT '站内通知成功写入后的发送时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_knowledge_review_reminder_user_node (user_id, node_id),
    INDEX idx_knowledge_review_reminder_due (status, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点指定复习提醒';
