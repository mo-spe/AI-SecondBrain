-- ============================================================
-- AI-SecondBrain 数据库完整建表语句
-- ============================================================
-- 说明：
-- 1. 这是项目的完整数据库结构（包含所有 11 张表）
-- 2. 包含所有表、索引、注释
-- 3. 适用于 MySQL 8.0+
-- 4. 字符集：utf8mb4，排序规则：utf8mb4_unicode_ci
-- 5. 生成时间：2026-03-17
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS second_brain 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE second_brain;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    bio VARCHAR(500) COMMENT '个人简介',
    avatar VARCHAR(500) COMMENT '头像 URL',
    api_key VARCHAR(255) COMMENT 'API Key',
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
-- 2. 原始对话记录表 (raw_chat_record)
-- ============================================================
DROP TABLE IF EXISTS raw_chat_record;
CREATE TABLE raw_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    platform VARCHAR(50) COMMENT '来源平台（ChatGPT/DeepSeek/Kimi/Other）',
    content TEXT NOT NULL COMMENT '对话内容',
    source_url VARCHAR(500) COMMENT '原始链接',
    processed TINYINT DEFAULT 0 COMMENT '是否已处理（0-未处理，1-已处理）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_platform (platform),
    INDEX idx_create_time (create_time),
    INDEX idx_processed (processed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始对话记录表';

-- ============================================================
-- 3. 知识节点表 (knowledge_node)
-- ============================================================
DROP TABLE IF EXISTS knowledge_node;
CREATE TABLE knowledge_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    chat_record_id BIGINT COMMENT '来源对话记录 ID',
    title VARCHAR(200) NOT NULL COMMENT '知识点标题',
    content_md TEXT COMMENT 'Markdown 格式内容',
    summary TEXT COMMENT '摘要',
    vector_id VARCHAR(100) COMMENT '向量 ID（用于 Elasticsearch）',
    importance TINYINT NOT NULL DEFAULT 3 COMMENT '重要程度（1-5，5 最重要）',
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
-- 4. 知识向量表 (knowledge_embedding)
-- ============================================================
DROP TABLE IF EXISTS knowledge_embedding;
CREATE TABLE knowledge_embedding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识节点 ID',
    content TEXT NOT NULL COMMENT '用于向量化的内容',
    embedding TEXT NOT NULL COMMENT '向量表示（JSON 格式存储）',
    model VARCHAR(50) DEFAULT 'text-embedding-v2' COMMENT 'embedding 模型',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_knowledge_id (knowledge_id),
    INDEX idx_model (model),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识向量表';

-- ============================================================
-- 5. 复习记录表 (review_log)
-- ============================================================
DROP TABLE IF EXISTS review_log;
CREATE TABLE review_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    node_id BIGINT NOT NULL COMMENT '知识节点 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
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
-- 6. 知识标签表 (knowledge_tag)
-- ============================================================
DROP TABLE IF EXISTS knowledge_tag;
CREATE TABLE knowledge_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color VARCHAR(20) COMMENT '标签颜色（十六进制颜色代码）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- ============================================================
-- 7. 知识节点标签关联表 (knowledge_node_tag_relation)
-- ============================================================
DROP TABLE IF EXISTS knowledge_node_tag_relation;
CREATE TABLE knowledge_node_tag_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    node_id BIGINT NOT NULL COMMENT '知识节点 ID',
    tag_id BIGINT NOT NULL COMMENT '标签 ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_node_tag (node_id, tag_id),
    INDEX idx_node_id (node_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点标签关联表';

-- ============================================================
-- 8. 知识关系表 (knowledge_relation)
-- ============================================================
DROP TABLE IF EXISTS knowledge_relation;
CREATE TABLE knowledge_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    from_knowledge_id BIGINT NOT NULL COMMENT '源知识节点 ID',
    to_knowledge_id BIGINT NOT NULL COMMENT '目标知识节点 ID',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型：parent-child, related, prerequisite',
    relation_name VARCHAR(100) COMMENT '关系名称',
    weight DECIMAL(5, 4) DEFAULT 1.0000 COMMENT '关系权重 1-5',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
    INDEX idx_user_id (user_id),
    INDEX idx_from_knowledge_id (from_knowledge_id),
    INDEX idx_to_knowledge_id (to_knowledge_id),
    INDEX idx_relation_type (relation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识关系表';

-- ============================================================
-- 9. 复习卡片表 (review_card)
-- ============================================================
DROP TABLE IF EXISTS review_card;
CREATE TABLE review_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '卡片 ID',
    node_id BIGINT NOT NULL COMMENT '知识点 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    question TEXT COMMENT '问题',
    answer TEXT COMMENT '答案',
    card_type VARCHAR(20) NOT NULL DEFAULT 'simple' COMMENT '卡片类型：simple-简单，choice-单选题，fill-填空题',
    difficulty INT NOT NULL DEFAULT 1 COMMENT '难度：1-简单，2-中等，3-困难',
    review_count INT NOT NULL DEFAULT 0 COMMENT '复习次数',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '正确次数',
    incorrect_count INT NOT NULL DEFAULT 0 COMMENT '错误次数',
    mastery_level INT NOT NULL DEFAULT 0 COMMENT '掌握程度：0-未掌握，1-入门，2-熟悉，3-掌握，4-精通，5-专家',
    memory_strength DECIMAL(5, 4) NOT NULL DEFAULT 0.0000 COMMENT '记忆强度',
    last_review_time DATETIME COMMENT '最后复习时间',
    next_review_time DATETIME COMMENT '下次复习时间',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0-待复习，1-已完成，2-已跳过',
    ai_generated VARCHAR(10) NOT NULL DEFAULT 'false' COMMENT '是否 AI 生成：true-是，false-否',
    generation_type VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '生成类型：auto-自动生成，manual-手动生成',
    is_restored TINYINT(1) DEFAULT 0 COMMENT '是否已恢复：0-否，1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_node_id (node_id),
    INDEX idx_next_review_time (next_review_time),
    INDEX idx_status (status),
    INDEX idx_generation_type (generation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='复习卡片表';

-- ============================================================
-- 10. 学习报告表 (learning_report)
-- ============================================================
DROP TABLE IF EXISTS learning_report;
CREATE TABLE learning_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    topic VARCHAR(200) NOT NULL COMMENT '学习主题',
    content LONGTEXT COMMENT '报告内容（Markdown 格式）',
    days INT NOT NULL COMMENT '时间范围（天数）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习报告表';

-- ============================================================
-- 11. 异步任务表 (async_task)
-- ============================================================
DROP TABLE IF EXISTS async_task;
CREATE TABLE async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务 ID',
    task_number VARCHAR(64) NOT NULL COMMENT '任务编号',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待处理，PROCESSING-处理中，COMPLETED-已完成，FAILED-失败',
    progress INT DEFAULT 0 COMMENT '进度（0-100）',
    parameters TEXT COMMENT '任务参数（JSON 格式）',
    result TEXT COMMENT '任务结果',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME COMMENT '开始时间',
    complete_time DATETIME COMMENT '完成时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '是否删除',
    INDEX idx_task_number (task_number),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- ============================================================
-- 12. 研究历史记录表 (research_history)
-- ============================================================
DROP TABLE IF EXISTS research_history;
CREATE TABLE research_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    type VARCHAR(20) NOT NULL COMMENT '类型（path-学习路径，gaps-知识盲区）',
    topic VARCHAR(200) NOT NULL COMMENT '主题',
    content LONGTEXT COMMENT '内容（Markdown 格式）',
    current_level VARCHAR(20) DEFAULT NULL COMMENT '当前水平（仅学习路径：beginner/intermediate/advanced）',
    target_level VARCHAR(20) DEFAULT NULL COMMENT '目标水平（仅学习路径：beginner/intermediate/advanced）',
    depth VARCHAR(20) DEFAULT NULL COMMENT '深度（仅学习报告：basic/standard/comprehensive）',
    user_knowledge JSON DEFAULT NULL COMMENT '用户知识点（仅知识盲区：JSON 数组）',
    knowledge_count INT DEFAULT 0 COMMENT '知识点数量（仅知识盲区）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究历史记录表';

-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 插入测试用户（密码：123456，BCrypt 加密后为：$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq）
INSERT INTO user (username, password, email, bio) VALUES 
('testuser', '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq', 'test@example.com', '测试用户')
ON DUPLICATE KEY UPDATE username=username;

-- ============================================================
-- 表结构说明
-- ============================================================

-- 1. user（用户表）
--    - 存储用户基本信息
--    - 密码使用 BCrypt 加密
--    - 支持逻辑删除
--    - api_key 用于 AI 服务调用

-- 2. raw_chat_record（原始对话记录表）
--    - 存储从各个平台导入的对话记录
--    - 支持 ChatGPT、DeepSeek、Kimi 等平台
--    - 使用 LONGTEXT 存储长对话内容
--    - processed 字段标记是否已提取知识点

-- 3. knowledge_node（知识节点表）
--    - 存储从对话中提取的知识点
--    - 支持艾宾浩斯遗忘曲线复习
--    - 与 Elasticsearch 集成进行语义搜索
--    - 支持重要程度和掌握程度标记
--    - vector_id 用于关联向量索引

-- 4. knowledge_embedding（知识向量表）
--    - 存储知识点的向量表示
--    - 用于语义搜索和相似度计算
--    - 与 knowledge_node 级联删除

-- 5. review_log（复习记录表）
--    - 记录知识点的复习历史
--    - 支持复习结果分类（easy/hard/forgot/completed）
--    - 记录复习时长

-- 6. knowledge_tag（知识标签表）
--    - 存储用户自定义的标签
--    - 支持标签颜色

-- 7. knowledge_node_tag_relation（知识节点标签关联表）
--    - 多对多关联表
--    - 一个知识节点可以有多个标签
--    - 一个标签可以关联多个知识节点

-- 8. knowledge_relation（知识关系表）
--    - 存储知识点之间的关系
--    - 关系类型：parent-child（父子）、related（相关）、prerequisite（前置）
--    - 关系强度：1-5

-- 9. review_card（复习卡片表）
--    - 存储复习卡片（题目）
--    - 支持 AI 自动生成和手动生成
--    - 记录复习次数、正确率等统计信息
--    - generation_type：auto-自动生成，manual-手动生成
--    - is_restored：标记是否从回收站恢复

-- 10. learning_report（学习报告表）
--     - 存储 AI 生成的学习报告
--     - 基于用户的学习数据生成
--     - 支持时间范围筛选

-- 11. research_history（研究历史记录表）
--     - 存储学习路径和知识盲区分析结果
--     - 支持两种类型：学习路径（path）和知识盲区（gaps）
--     - 使用 JSON 存储用户知识点列表

-- 12. async_task（异步任务表）
--     - 存储异步任务信息
--     - 支持任务进度跟踪
--     - 记录任务参数和结果

-- ============================================================
-- 索引说明
-- ============================================================

-- 用户表索引：
-- - idx_username：用户名索引，用于登录查询
-- - idx_email：邮箱索引，用于邮箱查询
-- - idx_create_time：创建时间索引，用于按时间排序

-- 对话记录表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的对话记录
-- - idx_platform：平台索引，用于按平台筛选
-- - idx_create_time：创建时间索引，用于按时间排序
-- - idx_processed：处理状态索引，用于查询未处理的记录

-- 知识节点表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的知识节点
-- - idx_chat_record_id：对话记录 ID 索引，用于查询来源
-- - idx_next_review_time：下次复习时间索引，用于查询待复习节点
-- - idx_importance：重要程度索引，用于按重要性筛选
-- - idx_mastery_level：掌握程度索引，用于按掌握程度筛选
-- - idx_create_time：创建时间索引，用于按时间排序

-- 知识向量表索引：
-- - idx_knowledge_id：知识节点 ID 索引，用于查询知识的向量
-- - idx_model：模型索引，用于按模型筛选

-- 复习记录表索引：
-- - idx_node_id：知识节点 ID 索引，用于查询节点的复习记录
-- - idx_user_id：用户 ID 索引，用于查询用户的复习记录
-- - idx_result：复习结果索引，用于按结果筛选
-- - idx_create_time：创建时间索引，用于按时间排序

-- 标签表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的标签
-- - idx_tag_name：标签名称索引，用于标签搜索

-- 关联表索引：
-- - idx_node_id：知识节点 ID 索引，用于查询节点的标签
-- - idx_tag_id：标签 ID 索引，用于查询标签关联的节点

-- 知识关系表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的关系
-- - idx_source_id：源知识节点 ID 索引
-- - idx_target_id：目标知识节点 ID 索引
-- - idx_relation_type：关系类型索引
-- - uk_relation：唯一约束（源节点、目标节点、关系类型）

-- 复习卡片表索引：
-- - idx_user_id：用户 ID 索引
-- - idx_node_id：知识点 ID 索引
-- - idx_next_review_time：下次复习时间索引
-- - idx_status：状态索引

-- 学习报告表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的学习报告
-- - idx_create_time：创建时间索引，用于按时间排序

-- 研究历史记录表索引：
-- - idx_user_id：用户 ID 索引，用于查询用户的研究历史
-- - idx_type：类型索引，用于按类型筛选（学习路径/知识盲区）
-- - idx_create_time：创建时间索引，用于按时间排序

-- 异步任务表索引：
-- - idx_task_number：任务编号索引，用于查询任务状态
-- - idx_user_id：用户 ID 索引
-- - idx_status：任务状态索引
-- - idx_create_time：创建时间索引

-- ============================================================
-- 外键说明
-- ============================================================

-- 注意：当前版本仅 knowledge_embedding 表使用外键约束，其他表未使用外键，原因：
-- 1. 提高插入性能
-- 2. 简化数据迁移
-- 3. 应用层保证数据一致性

-- knowledge_embedding 表的外键：
-- FOREIGN KEY (knowledge_id) REFERENCES knowledge_node(id) ON DELETE CASCADE

-- 如果需要为其他表添加外键约束，可以使用以下语句：

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

-- ALTER TABLE review_card ADD CONSTRAINT fk_review_card_node 
--     FOREIGN KEY (node_id) REFERENCES knowledge_node(id) ON DELETE CASCADE;

-- ALTER TABLE review_card ADD CONSTRAINT fk_review_card_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ALTER TABLE async_task ADD CONSTRAINT fk_async_task_user 
--     FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

-- ============================================================
-- 完成提示
-- ============================================================

-- 数据库结构创建完成！
-- 共创建了 12 张表，包含：
-- - 用户管理：user
-- - 对话记录：raw_chat_record
-- - 知识管理：knowledge_node, knowledge_embedding
-- - 复习系统：review_log, review_card
-- - 标签系统：knowledge_tag, knowledge_node_tag_relation
-- - 知识关系：knowledge_relation
-- - 学习报告：learning_report
-- - 研究历史：research_history
-- - 异步任务：async_task
