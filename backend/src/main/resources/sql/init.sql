CREATE DATABASE IF NOT EXISTS second_brain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE second_brain;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    bio VARCHAR(500) COMMENT '个人简介',
    avatar VARCHAR(500) COMMENT '头像URL',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 原始对话记录表
CREATE TABLE IF NOT EXISTS raw_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    platform VARCHAR(50) COMMENT '来源平台 ChatGPT/DeepSeek/Kimi/Other',
    content LONGTEXT NOT NULL COMMENT '对话内容',
    source_url VARCHAR(500) COMMENT '原始链接',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始对话记录表';

-- 知识节点表
CREATE TABLE IF NOT EXISTS knowledge_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    chat_record_id BIGINT COMMENT '来源对话记录ID',
    title VARCHAR(200) NOT NULL COMMENT '知识点标题',
    content_md TEXT COMMENT 'Markdown格式内容',
    summary TEXT COMMENT '摘要',
    vector_id VARCHAR(100) COMMENT '向量ID（用于Elasticsearch）',
    importance TINYINT NOT NULL DEFAULT 3 COMMENT '重要程度 1-5',
    mastery_level TINYINT NOT NULL DEFAULT 0 COMMENT '掌握程度 0-100',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_review_time DATETIME COMMENT '上次复习时间',
    next_review_time DATETIME COMMENT '下次复习时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点表';

-- 复习记录表
CREATE TABLE IF NOT EXISTS review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    result VARCHAR(20) NOT NULL COMMENT '复习结果 easy/hard/forgot/completed',
    duration INT COMMENT '复习时长（秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习记录表';

-- 知识标签表（可选，用于标签分类）
CREATE TABLE IF NOT EXISTS knowledge_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(20) COMMENT '标签颜色',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- 知识节点标签关联表
CREATE TABLE IF NOT EXISTS knowledge_node_tag_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_node_tag (node_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点标签关联表';

-- 索引创建
-- 用户表索引
CREATE INDEX idx_username ON user(username);
CREATE INDEX idx_email ON user(email);
CREATE INDEX idx_create_time ON user(create_time);

-- 对话记录表索引
CREATE INDEX idx_user_id ON raw_chat_record(user_id);
CREATE INDEX idx_platform ON raw_chat_record(platform);
CREATE INDEX idx_create_time ON raw_chat_record(create_time);

-- 知识节点表索引
CREATE INDEX idx_user_id ON knowledge_node(user_id);
CREATE INDEX idx_chat_record_id ON knowledge_node(chat_record_id);
CREATE INDEX idx_next_review_time ON knowledge_node(next_review_time);
CREATE INDEX idx_importance ON knowledge_node(importance);
CREATE INDEX idx_mastery_level ON knowledge_node(mastery_level);
CREATE INDEX idx_create_time ON knowledge_node(create_time);

-- 复习记录表索引
CREATE INDEX idx_node_id ON review_log(node_id);
CREATE INDEX idx_user_id ON review_log(user_id);
CREATE INDEX idx_result ON review_log(result);
CREATE INDEX idx_create_time ON review_log(create_time);

-- 标签表索引
CREATE INDEX idx_user_id ON knowledge_tag(user_id);
CREATE INDEX idx_tag_name ON knowledge_tag(tag_name);

-- 关联表索引
CREATE INDEX idx_node_id ON knowledge_node_tag_relation(node_id);
CREATE INDEX idx_tag_id ON knowledge_node_tag_relation(tag_id);

-- 插入测试用户（密码：123456，BCrypt加密后）
INSERT INTO user (username, password, email, bio) VALUES 
('testuser', '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq', 'test@example.com', '测试用户')
ON DUPLICATE KEY UPDATE username=username;
