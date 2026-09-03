-- 知识社区 MVP 第二阶段：独立公开资料、关注与拉黑关系。
-- 前置条件：V10 已执行。本迁移不修改账户表和既有内容表。

CREATE TABLE IF NOT EXISTS community_user_profile (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID，同时作为社区资料主键',
    introduction VARCHAR(500) NOT NULL DEFAULT '' COMMENT '社区公开简介',
    expertise_tags_json VARCHAR(2000) NOT NULL DEFAULT '[]' COMMENT '擅长领域标签JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区用户公开资料';

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关注关系ID',
    follower_id BIGINT NOT NULL COMMENT '关注者用户ID',
    followed_id BIGINT NOT NULL COMMENT '被关注用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_follow (follower_id, followed_id),
    INDEX idx_user_follow_followed (followed_id, create_time),
    INDEX idx_user_follow_follower (follower_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户单向关注关系';

CREATE TABLE IF NOT EXISTS user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '拉黑关系ID',
    blocker_id BIGINT NOT NULL COMMENT '拉黑发起者用户ID',
    blocked_id BIGINT NOT NULL COMMENT '被拉黑用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_block (blocker_id, blocked_id),
    INDEX idx_user_block_blocked (blocked_id, create_time),
    INDEX idx_user_block_blocker (blocker_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户拉黑关系';
