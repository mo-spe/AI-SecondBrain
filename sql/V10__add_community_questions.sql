-- 知识社区 MVP 第一阶段：公开问题与知识型回答
-- 仅新增领域表，不修改现有知识广场表，便于后续评审统一互动模型。

CREATE TABLE IF NOT EXISTS community_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '问题ID',
    author_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(120) NOT NULL COMMENT '问题标题',
    content TEXT NOT NULL COMMENT '问题正文',
    tags_json VARCHAR(1000) NOT NULL DEFAULT '[]' COMMENT '知识领域标签JSON',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED',
    answer_count INT NOT NULL DEFAULT 0 COMMENT '回答数量冗余计数器',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数量冗余计数器',
    accepted_answer_id BIGINT NULL COMMENT '被采纳回答ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_cq_author (author_id),
    INDEX idx_cq_status_time (status, create_time),
    INDEX idx_cq_answer_count (answer_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识社区问题';

CREATE TABLE IF NOT EXISTS community_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '回答ID',
    question_id BIGINT NOT NULL COMMENT '所属问题ID',
    author_id BIGINT NOT NULL COMMENT '回答者ID',
    content MEDIUMTEXT NOT NULL COMMENT 'Markdown回答正文',
    knowledge_snapshots_json MEDIUMTEXT NOT NULL COMMENT '用户授权公开的知识点快照JSON',
    accepted TINYINT NOT NULL DEFAULT 0 COMMENT '是否被采纳',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_ca_question (question_id, accepted, create_time),
    INDEX idx_ca_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识社区回答';
