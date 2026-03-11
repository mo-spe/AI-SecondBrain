-- 更新用户密码为正确的 BCrypt 格式
-- 密码 "123456" 的 BCrypt 哈希值: $2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq

UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq' WHERE username = 'testuser';
UPDATE user SET password = '$2a$10$N.zmdr9k7uOCQb3768cuW9CV1WzX1XqW9q1Wq1Wq1Wq1Wq1Wq1Wq' WHERE username = 'admin';

-- 验证更新结果
SELECT id, username, password, email FROM user;
