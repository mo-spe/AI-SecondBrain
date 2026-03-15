-- 为现有知识节点生成测试关系
INSERT INTO knowledge_relation (user_id, source_id, target_id, relation_type, relation_strength, create_time, update_time)
SELECT 
    5 as user_id,
    MIN(id) as source_id,
    MAX(id) as target_id,
    'related' as relation_type,
    3 as relation_strength,
    NOW() as create_time,
    NOW() as update_time
FROM knowledge_node 
WHERE user_id = 5 AND deleted = 0
GROUP BY CEIL(id / 10)
LIMIT 50;

-- 检查插入的关系数量
SELECT COUNT(*) as relation_count FROM knowledge_relation WHERE user_id = 5;