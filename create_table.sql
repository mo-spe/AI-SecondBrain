CREATE TABLE IF NOT EXISTS research_history (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  type varchar(20) NOT NULL,
  topic varchar(200) NOT NULL,
  content longtext,
  current_level varchar(20) DEFAULT NULL,
  target_level varchar(20) DEFAULT NULL,
  user_knowledge json DEFAULT NULL,
  knowledge_count int DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted int NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_type (type),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;