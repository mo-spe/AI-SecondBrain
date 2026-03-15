-- 知识向量表（存储知识的向量表示）
CREATE TABLE IF NOT EXISTS knowledge_embedding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    knowledge_id BIGINT NOT NULL COMMENT '知识节点ID',
    content TEXT NOT NULL COMMENT '用于向量化的内容',
    embedding TEXT NOT NULL COMMENT '向量表示（JSON格式存储）',
    model VARCHAR(50) DEFAULT 'text-embedding-v2' COMMENT 'embedding模型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_knowledge_id (knowledge_id),
    INDEX idx_model (model),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识向量表';