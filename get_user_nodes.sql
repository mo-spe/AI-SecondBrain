-- 获取用户的知识节点ID
SELECT id, title, LEFT(summary, 100) as summary_preview 
FROM knowledge_node 
WHERE user_id = 5 
ORDER BY id 
LIMIT 20;