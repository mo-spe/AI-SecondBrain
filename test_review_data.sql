-- 检查并插入测试复习卡片数据
USE second_brain;

-- 检查表是否存在
SHOW TABLES LIKE 'review_card';

-- 如果表为空，插入测试数据
INSERT INTO review_card (node_id, user_id, question, answer, card_type, difficulty, review_count, correct_count, incorrect_count, average_accuracy, mastery_level, memory_strength, last_review_time, next_review_time, status, ai_generated, deleted, create_time, update_time)
SELECT 1, 1, 
       'Python的主要特点是什么？', 
       'Python的主要特点包括：简单易学、语法简洁、面向对象、动态类型、自动内存管理、丰富的标准库、跨平台等。', 
       'simple', 1, 0, 0, 0, 0.0, 0, 0.0, 
       NOW(), DATE_ADD(NOW(), INTERVAL 10 MINUTE), 0, 'true', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM review_card WHERE id = 1);

INSERT INTO review_card (node_id, user_id, question, answer, card_type, difficulty, review_count, correct_count, incorrect_count, average_accuracy, mastery_level, memory_strength, last_review_time, next_review_time, status, ai_generated, deleted, create_time, update_time)
SELECT 2, 1, 
       '关于微服务架构，以下哪项描述是正确的？\n\nA. 微服务架构将所有功能集成在一个单体应用中\nB. 微服务架构将应用拆分为多个独立的小服务\nC. 微服务架构只能使用Java开发\nD. 微服务架构不需要API网关\n\n正确答案：B\n解析：微服务架构将应用拆分为多个独立的小服务，每个服务专注于单一业务功能。', 
       'B', 
       'choice', 2, 0, 0, 0, 0.0, 0, 0.0, 
       NOW(), DATE_ADD(NOW(), INTERVAL 30 MINUTE), 0, 'true', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM review_card WHERE id = 2);

INSERT INTO review_card (node_id, user_id, question, answer, card_type, difficulty, review_count, correct_count, incorrect_count, average_accuracy, mastery_level, memory_strength, last_review_time, next_review_time, status, ai_generated, deleted, create_time, update_time)
SELECT 3, 1, 
       '在Spring Boot中，____注解用于标识一个类为配置类。', 
       '@Configuration', 
       'fill', 2, 0, 0, 0, 0.0, 0, 0.0, 
       NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 0, 'true', 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM review_card WHERE id = 3);

-- 查询插入的数据
SELECT id, node_id, user_id, card_type, difficulty, status, next_review_time 
FROM review_card 
WHERE deleted = 0 
ORDER BY id;
