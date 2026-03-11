CREATE TABLE `research_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(20) NOT NULL COMMENT '类型（path-学习路径，gaps-知识盲区）',
  `topic` varchar(200) NOT NULL COMMENT '主题',
  `content` longtext COMMENT '内容',
  `current_level` varchar(20) DEFAULT NULL COMMENT '当前水平（仅学习路径）',
  `target_level` varchar(20) DEFAULT NULL COMMENT '目标水平（仅学习路径）',
  `user_knowledge` json DEFAULT NULL COMMENT '用户知识点（仅知识盲区）',
  `knowledge_count` int DEFAULT 0 COMMENT '知识点数量（仅知识盲区）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研究历史记录表';
