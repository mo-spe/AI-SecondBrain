-- 重新创建知识向量表（使用TEXT存储JSON格式向量）
DROP TABLE IF EXISTS knowledge_embedding;

CREATE TABLE knowledge_embedding (
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

-- 重新创建知识关系表（使用正确的字段名）
DROP TABLE IF EXISTS knowledge_relation;

CREATE TABLE knowledge_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    from_knowledge_id BIGINT NOT NULL COMMENT '起始知识ID',
    to_knowledge_id BIGINT NOT NULL COMMENT '目标知识ID',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型：contains/depends/related/inherits/implements',
    relation_name VARCHAR(100) COMMENT '关系名称',
    weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '关系权重',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_id (user_id),
    INDEX idx_from (from_knowledge_id),
    INDEX idx_to (to_knowledge_id),
    INDEX idx_type (relation_type),
    FOREIGN KEY (from_knowledge_id) REFERENCES knowledge_node(id) ON DELETE CASCADE,
    FOREIGN KEY (to_knowledge_id) REFERENCES knowledge_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识关系表';