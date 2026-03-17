-- 添加字段区分手动和自动生成的复习卡片
-- 执行时间：2026-03-15

ALTER TABLE review_card 
ADD COLUMN generation_type VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '生成类型：auto-自动生成（正式复习）, manual-手动生成（额外练习）' AFTER ai_generated;

-- 添加索引优化查询
ALTER TABLE review_card 
ADD INDEX idx_generation_type (generation_type),
ADD INDEX idx_status_generation_type (status, generation_type);