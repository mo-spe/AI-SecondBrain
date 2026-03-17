-- AI-SecondBrain v2.0 Database Schema
-- Generated: 2026-03-17
-- Note: All COMMENT clauses removed to avoid encoding issues

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Table structure for async_task
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_number` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `task_type` varchar(50) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `progress` int DEFAULT '0',
  `parameters` text,
  `result` text,
  `error_message` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `start_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_number` (`task_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_embedding
DROP TABLE IF EXISTS `knowledge_embedding`;
CREATE TABLE `knowledge_embedding` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_id` bigint NOT NULL,
  `content` text NOT NULL,
  `embedding` text NOT NULL,
  `model` varchar(50) DEFAULT 'text-embedding-v2',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`),
  KEY `idx_model` (`model`),
  CONSTRAINT `knowledge_embedding_ibfk_1` FOREIGN KEY (`knowledge_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_node
DROP TABLE IF EXISTS `knowledge_node`;
CREATE TABLE `knowledge_node` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `chat_record_id` bigint DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_md` text COLLATE utf8mb4_unicode_ci,
  `summary` text COLLATE utf8mb4_unicode_ci,
  `vector_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `importance` tinyint NOT NULL DEFAULT '3',
  `mastery_level` tinyint NOT NULL DEFAULT '0',
  `review_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_review_time` datetime DEFAULT NULL,
  `next_review_time` datetime DEFAULT NULL,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chat_record_id` (`chat_record_id`),
  KEY `idx_next_review_time` (`next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_node_tag_relation
DROP TABLE IF EXISTS `knowledge_node_tag_relation`;
CREATE TABLE `knowledge_node_tag_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_knowledge_tag` (`knowledge_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_tag
DROP TABLE IF EXISTS `knowledge_tag`;
CREATE TABLE `knowledge_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_relation
DROP TABLE IF EXISTS `knowledge_relation`;
CREATE TABLE `knowledge_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_node_id` bigint NOT NULL,
  `to_node_id` bigint NOT NULL,
  `relation_type` varchar(50) NOT NULL,
  `weight` decimal(5,2) DEFAULT '1.00',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_from_node` (`from_node_id`),
  KEY `idx_to_node` (`to_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for qa_session
DROP TABLE IF EXISTS `qa_session`;
CREATE TABLE `qa_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `question` text NOT NULL,
  `answer` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for qa_feedback
DROP TABLE IF EXISTS `qa_feedback`;
CREATE TABLE `qa_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `rating` tinyint DEFAULT NULL,
  `feedback` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for raw_chat_record
DROP TABLE IF EXISTS `raw_chat_record`;
CREATE TABLE `raw_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for relation_type_dict
DROP TABLE IF EXISTS `relation_type_dict`;
CREATE TABLE `relation_type_dict` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type_name` varchar(50) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_name` (`type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for review_card
DROP TABLE IF EXISTS `review_card`;
CREATE TABLE `review_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `knowledge_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'NEW',
  `next_review_time` datetime DEFAULT NULL,
  `last_review_time` datetime DEFAULT NULL,
  `review_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_next_review_time` (`next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for review_log
DROP TABLE IF EXISTS `review_log`;
CREATE TABLE `review_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_card_id` bigint NOT NULL,
  `result` varchar(20) NOT NULL,
  `duration_seconds` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_review_card_id` (`review_card_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for learning_report
DROP TABLE IF EXISTS `learning_report`;
CREATE TABLE `learning_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `report_date` date NOT NULL,
  `review_count` int DEFAULT '0',
  `new_count` int DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for research_history
DROP TABLE IF EXISTS `research_history`;
CREATE TABLE `research_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `query` text NOT NULL,
  `result` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for user
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `api_key` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
