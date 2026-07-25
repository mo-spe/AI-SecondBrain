-- V9: 工作区共享复习卡片 — 题目池 + 个人副本 两层架构
-- 关联 PRD: docs/prd/workspace-review-card-pool-prd.md

-- 题目池表（工作区级模板，同一知识点在工作区内只生成一次）
CREATE TABLE review_card_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '关联知识点ID',
    workspace_id BIGINT NOT NULL COMMENT '所属工作区ID',
    question TEXT COMMENT '题目内容',
    answer TEXT COMMENT '正确答案',
    card_type VARCHAR(20) NOT NULL DEFAULT 'choice' COMMENT '题型: choice/fill/essay/judge',
    difficulty INT NOT NULL DEFAULT 1 COMMENT '初始难度 1-5',
    generation_type VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '生成方式: auto/manual',
    create_user_id BIGINT NOT NULL COMMENT '创建者（操作确认入库的人）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_pool_ws (workspace_id),
    INDEX idx_pool_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 个人副本表（每用户独立复习进度，通过 pool_id 关联池子模板）
CREATE TABLE user_review_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id BIGINT DEFAULT NULL COMMENT 'FK→review_card_pool.id，NULL=无池子模板的个人卡片',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    workspace_id BIGINT NOT NULL COMMENT '工作区ID',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '正确次数',
    incorrect_count INT NOT NULL DEFAULT 0 COMMENT '错误次数',
    mastery_level INT NOT NULL DEFAULT 0 COMMENT '掌握程度 0-5',
    memory_strength DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '记忆强度 0-1',
    last_review_time DATETIME NULL DEFAULT NULL COMMENT '上次复习时间',
    next_review_time DATETIME NULL DEFAULT NULL COMMENT '下次复习时间',
    status INT NOT NULL DEFAULT 0 COMMENT '0=待复习, 1=已掌握',
    is_archived TINYINT DEFAULT 0 COMMENT '是否归档（重新加入后旧副本标1）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_urc_pool (pool_id),
    INDEX idx_urc_user (user_id),
    INDEX idx_urc_ws (workspace_id),
    INDEX idx_urc_next_review (next_review_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
