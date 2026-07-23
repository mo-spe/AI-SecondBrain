-- ============================================
-- V1.0: 多租户 RBAC 数据库迁移脚本
-- 日期: 2026-07-23
-- ============================================

-- 1. 工作区表
CREATE TABLE IF NOT EXISTS workspace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工作区ID',
    name VARCHAR(100) NOT NULL COMMENT '工作区名称',
    description VARCHAR(500) DEFAULT '' COMMENT '工作区描述',
    owner_id BIGINT NOT NULL COMMENT '创建者用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    INDEX idx_workspace_owner (owner_id),
    INDEX idx_workspace_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作区/租户';

-- 2. 工作区成员表
CREATE TABLE IF NOT EXISTS workspace_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员记录ID',
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '工作区角色：owner/admin/editor/viewer',
    joined_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    UNIQUE KEY uk_workspace_user (workspace_id, user_id),
    INDEX idx_ws_member_user (user_id),
    INDEX idx_ws_member_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作区成员';

-- 3. 补充缺失的 chat_session 表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) DEFAULT '' COMMENT '会话标题',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    INDEX idx_cs_user (user_id),
    INDEX idx_cs_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 4. 补充缺失的 chat_message 表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '关联会话ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色：user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0-未删除，1-已删除',
    INDEX idx_cm_session (session_id),
    INDEX idx_cm_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 5. user 表增加角色和状态
-- ALTER TABLE `user`
    -- ADD COLUMN role VARCHAR(20) DEFAULT 'user' COMMENT '平台角色：super_admin/user',
    -- ADD COLUMN status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用';

-- 6. 业务表增加 workspace_id
ALTER TABLE knowledge_node      ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE knowledge_relation  ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE review_card         ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE review_log          ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE chat_session        ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE chat_message        ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE learning_report     ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE async_task          ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE raw_chat_record     ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';
ALTER TABLE research_history    ADD COLUMN workspace_id BIGINT DEFAULT NULL COMMENT '工作区ID';

-- 7. workspace_id 索引
CREATE INDEX idx_kn_ws  ON knowledge_node(workspace_id);
CREATE INDEX idx_kr_ws  ON knowledge_relation(workspace_id);
CREATE INDEX idx_rc_ws  ON review_card(workspace_id);
CREATE INDEX idx_rl_ws  ON review_log(workspace_id);
CREATE INDEX idx_cs_ws  ON chat_session(workspace_id);
CREATE INDEX idx_cm_ws  ON chat_message(workspace_id);
CREATE INDEX idx_lr_ws  ON learning_report(workspace_id);
CREATE INDEX idx_at_ws  ON async_task(workspace_id);
CREATE INDEX idx_rcr_ws ON raw_chat_record(workspace_id);
CREATE INDEX idx_rh_ws  ON research_history(workspace_id);
