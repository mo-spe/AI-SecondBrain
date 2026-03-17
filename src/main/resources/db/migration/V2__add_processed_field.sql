-- 为 raw_chat_record 表添加 processed 字段
-- 用于标记消息是否已被 Kafka 消费者处理，避免重复消费

-- 添加 processed 字段，默认值为 0（未处理）
ALTER TABLE raw_chat_record 
ADD COLUMN processed TINYINT DEFAULT 0 COMMENT '是否已处理：0-未处理，1-已处理';

-- 将已有的记录标记为已处理（避免历史消息被重复处理）
UPDATE raw_chat_record 
SET processed = 1 
WHERE deleted = 0;

-- 添加索引，提高查询效率
CREATE INDEX idx_processed ON raw_chat_record(processed, deleted);
