-- 为 knowledge_node 表添加 need_review 列
-- 用于标识知识点是否纳入复习目标
ALTER TABLE knowledge_node 
ADD COLUMN need_review INT DEFAULT 0 COMMENT '是否纳入复习目标（0=未纳入，1=已纳入）';

-- 为 pending_knowledge 表添加 need_review 列（如果尚未存在）
-- 用于在 AI 提取阶段标记知识点是否需要生成复习卡片
ALTER TABLE pending_knowledge 
ADD COLUMN need_review INT DEFAULT 0 COMMENT '是否需要复习（0=不需要，1=需要）';