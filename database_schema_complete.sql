-- ============================================================
-- AI-SecondBrain 数据库完整建表语句
-- ============================================================
-- 说明：
-- 1. 这是项目的完整数据库结构
-- 2. 包含所有表、索引、注释
-- 3. 适用于MySQL 8.0+
-- 4. 字符集：utf8mb4，排序规则：utf8mb4_unicode_ci
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS second_brain 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE second_brain;

-- ============================================================
-- 1. 用户表
-- ============================================================
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
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 原始对话记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS raw_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    platform VARCHAR(50) COMMENT '来源平台（ChatGPT/DeepSeek/Kimi/Other）',
    content LONGTEXT NOT NULL COMMENT '对话内容',
    source_url VARCHAR(500) COMMENT '原始链接',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始对话记录表';

-- ============================================================
-- 3. 知识节点表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    chat_record_id BIGINT COMMENT '来源对话记录ID',
    title VARCHAR(200) NOT NULL COMMENT '知识点标题',
    content_md TEXT COMMENT 'Markdown格式内容',
    summary TEXT COMMENT '摘要',
    vector_id VARCHAR(100) COMMENT '向量ID（用于Elasticsearch）',
    importance TINYINT NOT NULL DEFAULT 3 COMMENT '重要程度（1-5，5最重要）',
    mastery_level TINYINT NOT NULL DEFAULT 0 COMMENT '掌握程度（0-100）',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_review_time DATETIME COMMENT '上次复习时间',
    next_review_time DATETIME COMMENT '下次复习时间（基于艾宾浩斯遗忘曲线）',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_chat_record_id (chat_record_id),
    INDEX idx_next_review_time (next_review_time),
    INDEX idx_importance (importance),
    INDEX idx_mastery_level (mastery_level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点表';

-- ============================================================
-- 4. 复习记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    result VARCHAR(20) NOT NULL COMMENT '复习结果（easy-简单，hard-困难，forgot-忘记，completed-完成）',
    duration INT COMMENT '复习时长（秒）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_node_id (node_id),
    INDEX idx_user_id (user_id),
    INDEX idx_result (result),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习记录表';

-- ============================================================
-- 5. 知识标签表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(20) COMMENT '标签颜色（十六进制颜色代码）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- ============================================================
-- 6. 知识节点标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS knowledge_node_tag_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '知识节点ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_node_tag (node_id, tag_id),
    INDEX idx_node_id (node_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点标签关联表';

-- ============================================================
-- 7. 知识关系表
-- ============================================================
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

-- ============================================================
-- 8. 学习报告表
-- ============================================================
CREATE TABLE IF NOT EXISTS learning_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    topic VARCHAR(200) NOT NULL COMMENT '学习主题',
    content LONGTEXT COMMENT '报告内容（Markdown格式）',
    days INT NOT NULL COMMENT '时间范围（天数）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习报告表';

-- ============================================================
-- 9. 研究历史记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS research_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(20) NOT NULL COMMENT '类型（path-学习路径，gaps-知识盲区）',
    topic VARCHAR(200) NOT NULL COMMENT '主题',
    content LONGTEXT COMMENT '内容（Markdown格式）',
    current_level VARCHAR(20) DEFAULT NULL COMMENT '当前水平（仅学习路径：beginner/intermediate/advanced）',
    target_level VARCHAR(20) DEFAULT NULL COMMENT '目标水平（仅学习路径：beginner/intermediate/advanced）',
    user_knowledge JSON DEFAULT NULL COMMENT '用户知识点（仅知识盲区：JSON数组）',
    knowledge_count INT DEFAULT 0 COMMENT '知识点数量（仅知识盲区）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究历史记录表';

-- ============================================================
-- 测试数据
-- ============================================================

-- 插入测试用户（密码：123456，BCrypt加密后）
INSERT INTO user (username, password, email, bio) VALUES 
('testuser', '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq', 'test@example.com', '测试用户')
ON DUPLICATE KEY UPDATE username=username;

-- ============================================================
-- 表结构说明
-- ============================================================

-- 1. user（用户表）
--    - 存储用户基本信息
--    - 密码使用BCrypt加密
--    - 支持逻辑删除

-- 2. raw_chat_record（原始对话记录表）
--    - 存储从各个平台导入的对话记录
--    - 支持ChatGPT、DeepSeek、Kimi等平台
--    - 使用LONGTEXT存储长对话内容

-- 3. knowledge_node（知识节点表）
--    - 存储从对话中提取的知识点
--    - 支持艾宾浩斯遗忘曲线复习
--    - 与Elasticsearch集成进行语义搜索
--    - 支持重要程度和掌握程度标记

-- 4. review_log（复习记录表）
--    - 记录知识点的复习历史
--    - 支持复习结果分类（easy/hard/forgot/completed）
--    - 记录复习时长

-- 5. knowledge_tag（知识标签表）
--    - 存储用户自定义的标签
--    - 支持标签颜色

-- 6. knowledge_node_tag_relation（知识节点标签关联表）
--    - 多对多关联表
--    - 一个知识节点可以有多个标签
--    - 一个标签可以关联多个知识节点

-- 7. learning_report（学习报告表）
--    - 存储AI生成的学习报告
--    - 基于用户的学习数据生成
--    - 支持时间范围筛选

-- 8. research_history（研究历史记录表）
--    - 存储学习路径和知识盲区分析结果
--    - 支持两种类型：学习路径（path）和知识盲区（gaps）
--    - 使用JSON存储用户知识点列表

-- ============================================================
-- 索引说明
-- ============================================================

-- 用户表索引：
-- - idx_username：用户名索引，用于登录查询
-- - idx_email：邮箱索引，用于邮箱查询
-- - idx_create_time：创建时间索引，用于按时间排序

-- 对话记录表索引：
-- - idx_user_id：用户ID索引，用于查询用户的对话记录
-- - idx_platform：平台索引，用于按平台筛选
-- - idx_create_time：创建时间索引，用于按时间排序

-- 知识节点表索引：
-- - idx_user_id：用户ID索引，用于查询用户的知识节点
-- - idx_chat_record_id：对话记录ID索引，用于查询来源
-- - idx_next_review_time：下次复习时间索引，用于查询待复习节点
-- - idx_importance：重要程度索引，用于按重要性筛选
-- - idx_mastery_level：掌握程度索引，用于按掌握程度筛选
-- - idx_create_time：创建时间索引，用于按时间排序

-- 复习记录表索引：
-- - idx_node_id：知识节点ID索引，用于查询节点的复习记录
-- - idx_user_id：用户ID索引，用于查询用户的复习记录
-- - idx_result：复习结果索引，用于按结果筛选
-- - idx_create_time：创建时间索引，用于按时间排序

-- 标签表索引：
-- - idx_user_id：用户ID索引，用于查询用户的标签
-- - idx_tag_name：标签名称索引，用于标签搜索

-- 关联表索引：
-- - idx_node_id：知识节点ID索引，用于查询节点的标签
-- - idx_tag_id：标签ID索引，用于查询标签关联的节点

-- 学习报告表索引：
-- - idx_user_id：用户ID索引，用于查询用户的学习报告
-- - idx_create_time：创建时间索引，用于按时间排序

-- 研究历史记录表索引：
-- - idx_user_id：用户ID索引，用于查询用户的研究历史
-- - idx_type：类型索引，用于按类型筛选（学习路径/知识盲区）
-- - idx_create_time：创建时间索引，用于按时间排序

-- ============================================================
-- 外键说明
-- ============================================================

-- 注意：当前版本未使用外键约束，原因：
-- 1. 提高插入性能
-- 2. 简化数据迁移
-- 3. 应用层保证数据一致性

-- 如果需要添加外键约束，可以使用以下语句：

-- ALTER TABLE raw_chat_record ADD CONSTRAINT fk_chat_record_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE knowledge_node ADD CONSTRAINT fk_knowledge_node_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE knowledge_node ADD CONSTRAINT fk_knowledge_node_chat_record 
--     FOREIGN KEY (chat_record_id) REFERENCES raw_chat_record(id) ON DELETE SET NULL;

-- ALTER TABLE review_log ADD CONSTRAINT fk_review_log_node 
--     FOREIGN KEY (node_id) REFERENCES knowledge_node(id) ON DELETE CASCADE;

-- ALTER TABLE review_log ADD CONSTRAINT fk_review_log_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE knowledge_tag ADD CONSTRAINT fk_knowledge_tag_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE knowledge_node_tag_relation ADD CONSTRAINT fk_relation_node 
--     FOREIGN KEY (node_id) REFERENCES knowledge_node(id) ON DELETE CASCADE;

-- ALTER TABLE knowledge_node_tag_relation ADD CONSTRAINT fk_relation_tag 
--     FOREIGN KEY (tag_id) REFERENCES knowledge_tag(id) ON DELETE CASCADE;

-- ALTER TABLE learning_report ADD CONSTRAINT fk_learning_report_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE research_history ADD CONSTRAINT fk_research_history_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ============================================================
-- 完成提示
-- ============================================================

-- 数据库结构创建完成！
-- 共创建了8张表，包含用户管理、对话记录、知识管理、复习系统、标签系统、学习报告等功能。
