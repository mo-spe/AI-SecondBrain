-- AI-SecondBrain v2.0 Database Schema
-- Generated: 2026-03-17 20:31:13
-- Note: All COMMENT clauses removed to avoid encoding issues

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Table structure for async_task
DROP TABLE IF EXISTS \sync_task\;
CREATE TABLE \sync_task\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \	ask_number\ varchar(64) NOT NULL,
  \user_id\ bigint NOT NULL,
  \	ask_type\ varchar(50) NOT NULL,
  \status\ varchar(20) NOT NULL DEFAULT 'PENDING',
  \progress\ int DEFAULT '0',
  \parameters\ text,
  \esult\ text,
  \error_message\ text,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \start_time\ datetime DEFAULT NULL,
  \complete_time\ datetime DEFAULT NULL,
  \deleted\ int NOT NULL DEFAULT '0',
  PRIMARY KEY (\id\),
  UNIQUE KEY \	ask_number\ (\	ask_number\),
  KEY \idx_user_id\ (\user_id\),
  KEY \idx_status\ (\status\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_embedding
DROP TABLE IF EXISTS \knowledge_embedding\;
CREATE TABLE \knowledge_embedding\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \knowledge_id\ bigint NOT NULL,
  \content\ text NOT NULL,
  \embedding\ text NOT NULL,
  \model\ varchar(50) DEFAULT 'text-embedding-v2',
  \update_time\ datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_knowledge_id\ (\knowledge_id\),
  KEY \idx_model\ (\model\),
  CONSTRAINT \knowledge_embedding_ibfk_1\ FOREIGN KEY (\knowledge_id\) REFERENCES \knowledge_node\ (\id\) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_node
DROP TABLE IF EXISTS \knowledge_node\;
CREATE TABLE \knowledge_node\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \chat_record_id\ bigint DEFAULT NULL,
  \	itle\ varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  \content_md\ text COLLATE utf8mb4_unicode_ci,
  \summary\ text COLLATE utf8mb4_unicode_ci,
  \ector_id\ varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  \importance\ tinyint NOT NULL DEFAULT '3',
  \mastery_level\ tinyint NOT NULL DEFAULT '0',
  \eview_count\ int NOT NULL DEFAULT '0',
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \update_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \last_review_time\ datetime DEFAULT NULL,
  \
ext_review_time\ datetime DEFAULT NULL,
  \deleted\ tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\),
  KEY \idx_chat_record_id\ (\chat_record_id\),
  KEY \idx_next_review_time\ (\
ext_review_time\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_node_tag_relation
DROP TABLE IF EXISTS \knowledge_node_tag_relation\;
CREATE TABLE \knowledge_node_tag_relation\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \knowledge_id\ bigint NOT NULL,
  \	ag_id\ bigint NOT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  UNIQUE KEY \uk_knowledge_tag\ (\knowledge_id\, \	ag_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_tag
DROP TABLE IF EXISTS \knowledge_tag\;
CREATE TABLE \knowledge_tag\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \
ame\ varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \deleted\ tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for knowledge_relation
DROP TABLE IF EXISTS \knowledge_relation\;
CREATE TABLE \knowledge_relation\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \rom_node_id\ bigint NOT NULL,
  \	o_node_id\ bigint NOT NULL,
  \elation_type\ varchar(50) NOT NULL,
  \weight\ decimal(5,2) DEFAULT '1.00',
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_from_node\ (\rom_node_id\),
  KEY \idx_to_node\ (\	o_node_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for qa_session
DROP TABLE IF EXISTS \qa_session\;
CREATE TABLE \qa_session\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \question\ text NOT NULL,
  \nswer\ text,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for qa_feedback
DROP TABLE IF EXISTS \qa_feedback\;
CREATE TABLE \qa_feedback\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \session_id\ bigint NOT NULL,
  \ating\ tinyint DEFAULT NULL,
  \eedback\ text,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_session_id\ (\session_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for raw_chat_record
DROP TABLE IF EXISTS \aw_chat_record\;
CREATE TABLE \aw_chat_record\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \ole\ varchar(20) NOT NULL,
  \content\ text NOT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \deleted\ tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for relation_type_dict
DROP TABLE IF EXISTS \elation_type_dict\;
CREATE TABLE \elation_type_dict\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \	ype_name\ varchar(50) NOT NULL,
  \description\ varchar(200) DEFAULT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  UNIQUE KEY \uk_type_name\ (\	ype_name\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for review_card
DROP TABLE IF EXISTS \eview_card\;
CREATE TABLE \eview_card\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \knowledge_id\ bigint NOT NULL,
  \user_id\ bigint NOT NULL,
  \status\ varchar(20) NOT NULL DEFAULT 'NEW',
  \
ext_review_time\ datetime DEFAULT NULL,
  \last_review_time\ datetime DEFAULT NULL,
  \eview_count\ int NOT NULL DEFAULT '0',
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\),
  KEY \idx_next_review_time\ (\
ext_review_time\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for review_log
DROP TABLE IF EXISTS \eview_log\;
CREATE TABLE \eview_log\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \eview_card_id\ bigint NOT NULL,
  \esult\ varchar(20) NOT NULL,
  \duration_seconds\ int DEFAULT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_review_card_id\ (\eview_card_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for learning_report
DROP TABLE IF EXISTS \learning_report\;
CREATE TABLE \learning_report\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \eport_date\ date NOT NULL,
  \eview_count\ int DEFAULT '0',
  \
ew_count\ int DEFAULT '0',
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\),
  KEY \idx_report_date\ (\eport_date\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for research_history
DROP TABLE IF EXISTS \esearch_history\;
CREATE TABLE \esearch_history\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \user_id\ bigint NOT NULL,
  \query\ text NOT NULL,
  \esult\ text,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\id\),
  KEY \idx_user_id\ (\user_id\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table structure for user
DROP TABLE IF EXISTS \user\;
CREATE TABLE \user\ (
  \id\ bigint NOT NULL AUTO_INCREMENT,
  \username\ varchar(50) NOT NULL,
  \password\ varchar(255) NOT NULL,
  \email\ varchar(100) DEFAULT NULL,
  \pi_key\ varchar(255) DEFAULT NULL,
  \create_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \update_time\ datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  \deleted\ tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (\id\),
  UNIQUE KEY \uk_username\ (\username\)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
