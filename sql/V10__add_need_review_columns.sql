-- 补齐 need_review 字段
--
-- 背景：KnowledgeNode / PendingKnowledge 两个实体都有 needReview 字段，
-- 但 second_brain.sql 与 V8__add_pending_knowledge.sql 的建表语句里漏了对应的 need_review 列，
-- 导致知识点管理页（纳入复习目标 / 采集时选择是否生成复习卡片）查询报 Unknown column。
--
-- 全字段比对结论（entity vs information_schema）：全库仅缺这 2 列，其余一致。
--
-- 用法：mysql -h 127.0.0.1 -P 3307 -u root -p second_brain < V10__add_need_review_columns.sql
-- 若字段已存在会报 Duplicate column name，属正常，可忽略。

USE second_brain;

ALTER TABLE knowledge_node
    ADD COLUMN need_review TINYINT NOT NULL DEFAULT 0 COMMENT '是否纳入复习目标：0=未纳入，1=已纳入' AFTER source_id,
    ADD INDEX idx_kn_need_review (need_review);

ALTER TABLE pending_knowledge
    ADD COLUMN need_review TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要复习（0=不需要，1=需要）' AFTER status;
