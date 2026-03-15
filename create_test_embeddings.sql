-- 为知识节点创建测试向量数据
INSERT INTO knowledge_embedding (knowledge_id, content, embedding, model, create_time, update_time)
SELECT 
    id as knowledge_id,
    CONCAT(title, ' ', IFNULL(summary, ''), ' ', IFNULL(content_md, '')) as content,
    CONCAT('[', 
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4), ',',
        ROUND(RAND() * 2 - 1, 4)
    , ']') as embedding,
    'text-embedding-v2' as model,
    NOW() as create_time,
    NOW() as update_time
FROM knowledge_node 
WHERE user_id = 5 AND deleted = 0
LIMIT 50;

-- 检查插入的向量数量
SELECT COUNT(*) as embedding_count FROM knowledge_embedding WHERE knowledge_id IN (SELECT id FROM knowledge_node WHERE user_id = 5);