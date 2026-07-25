-- ============================================================
-- V8: 待确认知识点表
-- 采集对话后 AI 提取的知识点先进到此表，人工确认后才入库
-- ============================================================
CREATE TABLE IF NOT EXISTS pending_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    workspace_id BIGINT COMMENT '目标工作区ID（NULL=个人空间）',
    raw_chat_id BIGINT COMMENT '关联的原始对话ID',
    title VARCHAR(500) COMMENT '知识点标题',
    summary TEXT COMMENT '摘要',
    content TEXT COMMENT '详细内容',
    status TINYINT DEFAULT 0 COMMENT '状态：0=待确认，1=已确认入库，2=已丢弃',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待确认知识点表';
