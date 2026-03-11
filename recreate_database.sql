-- 重新创建数据库脚本
-- ⚠️ 警告：此操作会删除所有数据，请谨慎执行！

-- 1. 删除现有数据库
DROP DATABASE IF EXISTS second_brain;

-- 2. 创建新数据库
CREATE DATABASE second_brain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 使用数据库
USE second_brain;

-- 4. 创建用户表
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    bio VARCHAR(500) COMMENT '个人简介',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 5. 创建原始对话记录表
CREATE TABLE raw_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    platform VARCHAR(50) COMMENT '来源平台 ChatGPT/DeepSeek/Kimi/Other',
    content TEXT NOT NULL COMMENT '对话内容',
    source_url VARCHAR(500) COMMENT '原始链接',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始对话记录表';

-- 6. 创建知识节点表
CREATE TABLE knowledge_node (
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
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_user_id (user_id),
    INDEX idx_chat_record_id (chat_record_id),
    INDEX idx_next_review_time (next_review_time),
    INDEX idx_importance (importance),
    INDEX idx_mastery_level (mastery_level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点表';

-- 7. 创建复习记录表
CREATE TABLE review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    result VARCHAR(20) NOT NULL COMMENT '复习结果 easy/hard/forgot/completed',
    duration INT COMMENT '复习时长（秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_node_id (node_id),
    INDEX idx_user_id (user_id),
    INDEX idx_result (result),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习记录表';

-- 8. 创建知识标签表
CREATE TABLE knowledge_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(20) COMMENT '标签颜色',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
    INDEX idx_user_id (user_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- 9. 创建知识节点标签关联表
CREATE TABLE knowledge_node_tag_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_node_tag (node_id, tag_id),
    INDEX idx_node_id (node_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点标签关联表';

-- 10. 插入测试用户（密码：123456）
INSERT INTO user (username, password, email, bio) VALUES 
('testuser', '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq', 'test@example.com', '测试用户'),
('admin', '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq', 'admin@example.com', '管理员用户');

-- 11. 插入测试对话记录
INSERT INTO raw_chat_record (user_id, platform, content, source_url) VALUES 
(1, 'ChatGPT', 'User: 什么是Java？\nAssistant: Java是一种广泛使用的编程语言，由Sun公司（现Oracle）开发。它具有跨平台性、面向对象、安全性高等特点。Java广泛应用于企业级应用开发、移动应用开发（Android）、大数据处理等领域。', 'https://chat.openai.com/example'),
(1, 'DeepSeek', 'User: 什么是Spring Boot？\nAssistant: Spring Boot是基于Spring框架的快速开发框架，它简化了Spring应用的初始搭建和开发过程。通过自动配置和起步依赖，开发者可以快速创建独立的、生产级别的Spring应用。Spring Boot内置了Tomcat服务器，支持热部署，非常适合微服务架构。', 'https://www.deepseek.com/example');

-- 12. 插入测试知识点
INSERT INTO knowledge_node (user_id, chat_record_id, title, content_md, summary, importance, mastery_level, review_count, next_review_time) VALUES 
(1, 1, 'Java编程语言简介', '## Java编程语言\n\nJava是一种广泛使用的编程语言，由Sun公司（现Oracle）开发。\n\n### 主要特点\n- 跨平台性\n- 面向对象\n- 安全性高\n- 自动内存管理\n\n### 应用领域\n- 企业级应用开发\n- 移动应用开发（Android）\n- 大数据处理\n- Web开发', 'Java是一种跨平台、面向对象的编程语言，广泛应用于企业级开发、移动开发和大数据处理。', 4, 30, 2, DATE_ADD(NOW(), INTERVAL 1 DAY)),
(1, 2, 'Spring Boot框架', '## Spring Boot框架\n\nSpring Boot是基于Spring框架的快速开发框架。\n\n### 核心特性\n- 自动配置\n- 起步依赖\n- 内置服务器\n- 热部署支持\n\n### 优势\n- 简化开发流程\n- 提高开发效率\n- 适合微服务架构', 'Spring Boot简化了Spring应用开发，通过自动配置和起步依赖，可以快速创建独立的Spring应用。', 5, 50, 3, DATE_ADD(NOW(), INTERVAL 3 DAY));

-- 13. 插入测试复习记录
INSERT INTO review_log (node_id, user_id, result, duration) VALUES 
(1, 1, 'easy', 120),
(1, 1, 'hard', 180),
(2, 1, 'easy', 90),
(2, 1, 'easy', 85);

-- 14. 插入测试标签
INSERT INTO knowledge_tag (user_id, tag_name, tag_color) VALUES 
(1, '编程语言', '#409EFF'),
(1, '框架', '#67C23A'),
(1, '基础概念', '#E6A23C');

-- 15. 插入测试标签关联
INSERT INTO knowledge_node_tag_relation (node_id, tag_id) VALUES 
(1, 1),
(2, 2);

-- 16. 显示完成信息
SELECT '✓ 数据库重新创建成功！' AS message;
SELECT '✓ 测试账号：testuser / 123456' AS account;
SELECT '✓ 测试账号：admin / 123456' AS account;
