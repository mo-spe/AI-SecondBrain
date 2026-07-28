-- ============================================================
-- 知识标签层级化改造 — DDL
-- 为 knowledge_tag 表添加 parent_id 支持层级结构
-- ============================================================
ALTER TABLE knowledge_tag ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父标签ID，NULL表示顶级标签' AFTER tag_color;
ALTER TABLE knowledge_tag ADD INDEX idx_parent_id (parent_id);
