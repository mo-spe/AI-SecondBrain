-- ============================================================
-- 创建知识关系表
-- ============================================================

USE second_brain;

CREATE TABLE IF NOT EXISTS knowledge_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    source_id BIGINT NOT NULL COMMENT '源知识节点ID',
    target_id BIGINT NOT NULL COMMENT '目标知识节点ID',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型：parent-child, related, prerequisite',
    relation_strength INT DEFAULT 1 COMMENT '关系强度 1-5',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_source_id (source_id),
    INDEX idx_target_id (target_id),
    INDEX idx_relation_type (relation_type),
    UNIQUE KEY uk_relation (source_id, target_id, relation_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识关系表';

SELECT 'knowledge_relation表创建成功' AS status;
