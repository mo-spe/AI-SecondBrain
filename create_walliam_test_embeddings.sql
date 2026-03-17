DELETE FROM knowledge_embedding WHERE knowledge_id IN (SELECT id FROM knowledge_node WHERE user_id = 12);

INSERT INTO knowledge_embedding (knowledge_id, content, embedding, model, create_time, update_time) VALUES
(331, 'Java 接口的核心价值与设计模式应用', '[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0]', 'text-embedding-v2', NOW(), NOW()),
(332, 'Java 异常体系结构与分类处理机制', '[0.11,0.21,0.31,0.41,0.51,0.61,0.71,0.81,0.91,0.01]', 'text-embedding-v2', NOW(), NOW()),
(333, 'Java 集合框架的体系结构与核心实现类特性', '[0.12,0.22,0.32,0.42,0.52,0.62,0.72,0.82,0.92,0.02]', 'text-embedding-v2', NOW(), NOW()),
(334, '可变参数（Varargs）的语法机制与使用规范', '[0.13,0.23,0.33,0.43,0.53,0.63,0.73,0.83,0.93,0.03]', 'text-embedding-v2', NOW(), NOW()),
(335, '面向对象设计原则在实际项目中的综合应用', '[0.14,0.24,0.34,0.44,0.54,0.64,0.74,0.84,0.94,0.04]', 'text-embedding-v2', NOW(), NOW());

SELECT ke.knowledge_id, kn.title, LENGTH(ke.embedding) as embedding_length 
FROM knowledge_embedding ke 
JOIN knowledge_node kn ON ke.knowledge_id = kn.id 
WHERE kn.user_id = 12;
