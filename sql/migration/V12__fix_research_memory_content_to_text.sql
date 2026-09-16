-- ============================================================
-- V12: 修复 research_memory.content 列类型
-- ============================================================
-- 背景：
--   原设计 content 列为 JSON 类型，但前端 ResearchWorkspace.vue
--   通过 renderMd(m.content) 直接按 Markdown 渲染，二者不匹配。
--   且各 Agent 产出的知识背景/缺口/发现/结论/候选本身就是 Markdown 文本，
--   不是结构化 JSON，强制以 JSON 存储会导致写入失败
--   （Data truncation: Invalid JSON text）。
--
-- 方案：
--   将 content 列由 JSON 改为 LONGTEXT，允许任意 Markdown 文本，
--   与前端渲染逻辑保持一致。
--
-- 兼容性：
--   - 若历史记录中存在合法 JSON 字符串，转 TEXT 后仍以原字符串形式保留，
--     前端 renderMd 会将其当作普通文本渲染，不会报错。
--   - 不再有非法 JSON 写入失败的问题。
-- ============================================================

ALTER TABLE research_memory
    MODIFY COLUMN content LONGTEXT NOT NULL COMMENT '记忆内容（Markdown 文本）';
