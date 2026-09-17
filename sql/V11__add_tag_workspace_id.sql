-- 标签支持工作区共享
--
-- 问题：knowledge_node 早已按 workspace_id 隔离，但 knowledge_tag 只有 user_id，
-- 标签完全按用户隔离。导致工作区里 A 创建的标签，B 在标签区域看不到
-- —— 知识点上明明挂着标签，左侧标签树里却没有。
--
-- 方案：knowledge_tag 增加 workspace_id。
--   - workspace_id 非 NULL：工作区共享标签，对该工作区所有成员可见
--   - workspace_id 为 NULL：个人空间标签，仅本人可见
--
-- 用法：mysql -h 127.0.0.1 -P 3307 -u root -p second_brain < V11__add_tag_workspace_id.sql
-- 字段已存在时报 Duplicate column name 属正常，可忽略。

USE second_brain;

ALTER TABLE knowledge_tag
    ADD COLUMN workspace_id bigint NULL DEFAULT NULL
        COMMENT '工作区ID，NULL表示个人空间标签（工作区标签对所有成员共享）' AFTER parent_id,
    ADD INDEX idx_tag_ws (workspace_id);

-- 可选：把历史上「仅被单个工作区知识点使用」的个人标签，回填为工作区共享标签。
-- 若你的库里已有数据且希望旧标签在工作区可见，取消下面注释后再执行一次。
-- 注意：一个标签若被多个工作区的知识点使用，这里不会回填（保持为个人标签）。
--
-- UPDATE knowledge_tag t
--   SET t.workspace_id = (
--     SELECT n.workspace_id FROM knowledge_node_tag_relation r
--       JOIN knowledge_node n ON n.id = r.node_id
--      WHERE r.tag_id = t.id AND n.workspace_id IS NOT NULL
--      GROUP BY n.workspace_id
--     HAVING COUNT(DISTINCT n.workspace_id) = 1
--      LIMIT 1
--   )
-- WHERE t.deleted = 0
--   AND t.workspace_id IS NULL
--   AND EXISTS (
--     SELECT 1 FROM knowledge_node_tag_relation r
--       JOIN knowledge_node n ON n.id = r.node_id
--      WHERE r.tag_id = t.id AND n.workspace_id IS NOT NULL
--   );
