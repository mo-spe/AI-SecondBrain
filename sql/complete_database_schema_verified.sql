/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80026 (8.0.26)
 Source Host           : localhost:3306
 Source Schema         : second_brain

 Target Server Type    : MySQL
 Target Server Version : 80026 (8.0.26)
 File Encoding         : 65001

 Date: 04/09/2026 23:03:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for achievement
-- ----------------------------
DROP TABLE IF EXISTS `achievement`;
CREATE TABLE `achievement`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就代码（如 first_review）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就描述',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成就图标（Element Plus icon name）',
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成就分类：review/knowledge/streak/accuracy/mastery',
  `tier` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '等级：bronze/silver/gold/platinum',
  `trigger_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '触发类型：review_count/knowledge_count/streak_days/accuracy_rate/mastery_first',
  `trigger_value` int NOT NULL COMMENT '触发阈值',
  `points_reward` int NOT NULL DEFAULT 0 COMMENT '解锁奖励积分',
  `makeup_card_reward` int NOT NULL DEFAULT 0 COMMENT '解锁奖励补签卡数',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_tier`(`tier` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成就定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_execution
-- ----------------------------
DROP TABLE IF EXISTS `agent_execution`;
CREATE TABLE `agent_execution`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `execution_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '执行唯一键（幂等）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/FAILED/PAUSED/CANCELLED',
  `agent_chain_executed` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际执行的 Agent 序列',
  `current_agent` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前/最后执行的 Agent',
  `current_task_id` bigint NULL DEFAULT NULL COMMENT '当前/最后执行的任务 ID',
  `context_snapshot` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行上下文快照（用于恢复）',
  `checkpoint_at` datetime NULL DEFAULT NULL COMMENT '最近检查点时间',
  `total_duration_ms` bigint NULL DEFAULT NULL COMMENT '总执行时间（毫秒）',
  `token_usage_total` json NULL COMMENT '总 token 消耗',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '失败原因',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aexec_execution_key`(`execution_key` ASC) USING BTREE,
  INDEX `idx_aexec_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_aexec_execution_key`(`execution_key` ASC) USING BTREE,
  INDEX `idx_aexec_status`(`status` ASC) USING BTREE,
  INDEX `idx_aexec_project_status`(`project_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `agent_execution_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent 执行会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for agent_message
-- ----------------------------
DROP TABLE IF EXISTS `agent_message`;
CREATE TABLE `agent_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `execution_id` bigint NOT NULL COMMENT '所属执行会话 ID',
  `step_id` bigint NULL DEFAULT NULL COMMENT '关联 research_step ID',
  `agent_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用 LLM 的 Agent',
  `message_role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'system/user/assistant/tool',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '使用的模型名称',
  `provider_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI Provider code',
  `prompt_tokens` int NULL DEFAULT 0 COMMENT 'prompt token 数',
  `completion_tokens` int NULL DEFAULT 0 COMMENT 'completion token 数',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `metadata_json` json NULL COMMENT '额外元数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_amsg_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_amsg_step_id`(`step_id` ASC) USING BTREE,
  INDEX `idx_amsg_agent_name`(`agent_name` ASC) USING BTREE,
  INDEX `idx_amsg_execution_agent`(`execution_id` ASC, `agent_name` ASC, `create_time` ASC) USING BTREE,
  CONSTRAINT `agent_message_ibfk_1` FOREIGN KEY (`execution_id`) REFERENCES `agent_execution` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent LLM 对话记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_model
-- ----------------------------
DROP TABLE IF EXISTS `ai_model`;
CREATE TABLE `ai_model`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_id` bigint NOT NULL COMMENT '关联ai_provider.id',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型标识（gpt-4o/qwen-plus）',
  `display_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称（GPT-4o/通义千问Plus）',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `supported_scenarios` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '[\"chat\",\"extraction\",\"question_gen\",\"research\"]' COMMENT '适用场景JSON数组：chat/extraction/question_gen/embedding/research',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_provider_model`(`provider_id` ASC, `model_name` ASC) USING BTREE,
  INDEX `idx_provider_id`(`provider_id` ASC) USING BTREE,
  INDEX `idx_is_enabled`(`is_enabled` ASC) USING BTREE,
  CONSTRAINT `ai_model_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `ai_provider` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI模型列表表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_provider
-- ----------------------------
DROP TABLE IF EXISTS `ai_provider`;
CREATE TABLE `ai_provider`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务商标识（openai/qwen/deepseek/anthropic/gemini/kimi/zhipu/doubao/minimax）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '服务商显示名称',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'API基础地址',
  `api_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'openai_compatible' COMMENT 'API协议类型：openai_compatible/anthropic/gemini',
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务商Logo图标地址',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_is_enabled`(`is_enabled` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI服务商定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for async_task
-- ----------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'task ID',
  `task_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'task number',
  `user_id` bigint NOT NULL COMMENT 'user ID',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'task type',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT 'task status',
  `progress` int NULL DEFAULT 0 COMMENT 'progress',
  `parameters` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'task parameters',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'task result',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'error message',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `start_time` datetime NULL DEFAULT NULL COMMENT 'start time',
  `complete_time` datetime NULL DEFAULT NULL COMMENT 'complete time',
  `deleted` int NOT NULL DEFAULT 0 COMMENT 'deleted',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_number`(`task_number` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_task_number`(`task_number` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_at_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 60 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'async task table' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '关联会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息角色：user/assistant/system',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_cm_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '会话标题',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删除，1-已删除）',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_cs_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_answer
-- ----------------------------
DROP TABLE IF EXISTS `community_answer`;
CREATE TABLE `community_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回答ID',
  `question_id` bigint NOT NULL COMMENT '所属问题ID',
  `author_id` bigint NOT NULL COMMENT '回答者ID',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Markdown回答正文',
  `knowledge_snapshots_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户授权公开的知识点快照JSON',
  `accepted` tinyint NOT NULL DEFAULT 0 COMMENT '是否被采纳',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ca_question`(`question_id` ASC, `accepted` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ca_author`(`author_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识社区回答' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_question
-- ----------------------------
DROP TABLE IF EXISTS `community_question`;
CREATE TABLE `community_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '问题ID',
  `author_id` bigint NOT NULL COMMENT '发布者ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题正文',
  `tags_json` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]' COMMENT '知识领域标签JSON',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/CLOSED',
  `answer_count` int NOT NULL DEFAULT 0 COMMENT '回答数量冗余计数器',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览数量冗余计数器',
  `accepted_answer_id` bigint NULL DEFAULT NULL COMMENT '被采纳回答ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cq_author`(`author_id` ASC) USING BTREE,
  INDEX `idx_cq_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_cq_answer_count`(`answer_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识社区问题' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_user_profile
-- ----------------------------
DROP TABLE IF EXISTS `community_user_profile`;
CREATE TABLE `community_user_profile`  (
  `user_id` bigint NOT NULL COMMENT '用户ID，同时作为社区资料主键',
  `introduction` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '社区公开简介',
  `expertise_tags_json` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '[]' COMMENT '擅长领域标签JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区用户公开资料' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for daily_check_in
-- ----------------------------
DROP TABLE IF EXISTS `daily_check_in`;
CREATE TABLE `daily_check_in`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `check_in_date` date NOT NULL COMMENT '签到日期',
  `points_earned` int NOT NULL DEFAULT 0 COMMENT '本次签到获得积分',
  `streak_bonus_multiplier` decimal(3, 1) NOT NULL DEFAULT 1.0 COMMENT '连续签到加成倍数',
  `is_makeup` tinyint NOT NULL DEFAULT 0 COMMENT '是否为补签（0-正常签到，1-补签）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `check_in_date` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_check_in_date`(`check_in_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日签到记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for editing_lock
-- ----------------------------
DROP TABLE IF EXISTS `editing_lock`;
CREATE TABLE `editing_lock`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NOT NULL COMMENT '知识节点ID（唯一约束：同一节点只有一条锁记录）',
  `user_id` bigint NOT NULL COMMENT '持有锁的用户ID',
  `acquired_at` datetime NOT NULL COMMENT '锁获取时间',
  `expires_at` datetime NOT NULL COMMENT '锁过期时间（默认+15分钟）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_expires_at`(`expires_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '编辑锁' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_embedding
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_embedding`;
CREATE TABLE `knowledge_embedding`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `knowledge_id` bigint NOT NULL COMMENT '鐭ヨ瘑鑺傜偣ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐢ㄤ簬鍚戦噺鍖栫殑鍐呭?',
  `embedding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍚戦噺琛ㄧず锛圝SON鏍煎紡瀛樺偍锛',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'text-embedding-v2' COMMENT 'embedding妯″瀷',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
  INDEX `idx_model`(`model` ASC) USING BTREE,
  CONSTRAINT `knowledge_embedding_ibfk_1` FOREIGN KEY (`knowledge_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 211 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑鍚戦噺琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_node
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_node`;
CREATE TABLE `knowledge_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `chat_record_id` bigint NULL DEFAULT NULL COMMENT '来源对话记录ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点标题',
  `content_md` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Markdown格式内容',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '摘要',
  `vector_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '向量ID（用于Elasticsearch）',
  `importance` tinyint NOT NULL DEFAULT 3 COMMENT '重要程度 1-5',
  `mastery_level` tinyint NOT NULL DEFAULT 0 COMMENT '掌握程度 0-100',
  `review_count` int NOT NULL DEFAULT 0 COMMENT '复习次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_review_time` datetime NULL DEFAULT NULL COMMENT '上次复习时间',
  `next_review_time` datetime NULL DEFAULT NULL COMMENT '下次复习时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源类型：research/manual/rag_extraction/document_capture',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源 ID（关联 research_knowledge_candidate.id 或 research_source.id）',
  `need_review` int NULL DEFAULT 0 COMMENT '是否纳入复习目标（0=未纳入，1=已纳入）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_chat_record_id`(`chat_record_id` ASC) USING BTREE,
  INDEX `idx_next_review_time`(`next_review_time` ASC) USING BTREE,
  INDEX `idx_importance`(`importance` ASC) USING BTREE,
  INDEX `idx_mastery_level`(`mastery_level` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_kn_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 355 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识节点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_node_tag_relation
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_node_tag_relation`;
CREATE TABLE `knowledge_node_tag_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_id` bigint NOT NULL COMMENT '知识节点ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_node_tag`(`node_id` ASC, `tag_id` ASC) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识节点标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_relation
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_relation`;
CREATE TABLE `knowledge_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID',
  `from_knowledge_id` bigint NOT NULL COMMENT '璧峰?鐭ヨ瘑ID',
  `to_knowledge_id` bigint NOT NULL COMMENT '鐩?爣鐭ヨ瘑ID',
  `relation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍏崇郴绫诲瀷锛歝ontains/depends/related/inherits/implements',
  `relation_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍏崇郴鍚嶇О',
  `weight` decimal(5, 4) NULL DEFAULT 1.0000 COMMENT '鍏崇郴鏉冮噸',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '鏄?惁鍒犻櫎',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_from`(`from_knowledge_id` ASC) USING BTREE,
  INDEX `idx_to`(`to_knowledge_id` ASC) USING BTREE,
  INDEX `idx_type`(`relation_type` ASC) USING BTREE,
  INDEX `idx_kr_ws`(`workspace_id` ASC) USING BTREE,
  CONSTRAINT `knowledge_relation_ibfk_1` FOREIGN KEY (`from_knowledge_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `knowledge_relation_ibfk_2` FOREIGN KEY (`to_knowledge_id`) REFERENCES `knowledge_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 133 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑鍏崇郴琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_review_reminder
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_review_reminder`;
CREATE TABLE `knowledge_review_reminder`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鎻愰啋ID',
  `user_id` bigint NOT NULL COMMENT '鎺ユ敹鎻愰啋鐨勭敤鎴稩D',
  `node_id` bigint NOT NULL COMMENT '鍏宠仈鐭ヨ瘑鐐笽D',
  `scheduled_at` datetime NOT NULL COMMENT '鐢ㄦ埛鎸囧畾鐨勬彁閱掓椂闂',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SCHEDULED' COMMENT '鐘舵?锛歋CHEDULED鎴朣ENT',
  `sent_at` datetime NULL DEFAULT NULL COMMENT '绔欏唴閫氱煡鎴愬姛鍐欏叆鍚庣殑鍙戦?鏃堕棿',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_knowledge_review_reminder_user_node`(`user_id` ASC, `node_id` ASC) USING BTREE,
  INDEX `idx_knowledge_review_reminder_due`(`status` ASC, `scheduled_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑鐐规寚瀹氬?涔犳彁閱' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_revision
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_revision`;
CREATE TABLE `knowledge_revision`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NOT NULL COMMENT '知识节点ID',
  `user_id` bigint NOT NULL COMMENT '编辑用户ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '快照标题',
  `content_md` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '快照内容',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '快照摘要',
  `revision_num` int NOT NULL COMMENT '版本号（每个节点独立自增）',
  `change_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更说明',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识节点版本历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_tag
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_tag`;
CREATE TABLE `knowledge_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `tag_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签颜色',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父标签ID，NULL表示顶级标签',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_tag_name`(`tag_name` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for leaderboard_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `leaderboard_snapshot`;
CREATE TABLE `leaderboard_snapshot`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `period` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '排行榜周期：daily/weekly/monthly/all',
  `domain` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'all' COMMENT '领域（标签名称或 all）',
  `rank_position` int NOT NULL COMMENT '排名',
  `score` bigint NOT NULL COMMENT '当期综合得分',
  `snapshot_date` date NOT NULL COMMENT '快照日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_period_domain_date`(`user_id` ASC, `period` ASC, `domain` ASC, `snapshot_date` ASC) USING BTREE,
  INDEX `idx_period_domain_rank`(`period` ASC, `domain` ASC, `snapshot_date` ASC, `rank_position` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2753 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排行榜快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for learning_report
-- ----------------------------
DROP TABLE IF EXISTS `learning_report`;
CREATE TABLE `learning_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `topic` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `days` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` int NOT NULL DEFAULT 0,
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_lr_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型：like/comment/report_result',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通知内容',
  `target_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联目标类型：post',
  `target_id` bigint NULL DEFAULT NULL COMMENT '关联目标ID',
  `is_read` tinyint NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` datetime NOT NULL COMMENT '通知时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pending_knowledge
-- ----------------------------
DROP TABLE IF EXISTS `pending_knowledge`;
CREATE TABLE `pending_knowledge`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '目标工作区ID（NULL=个人空间）',
  `raw_chat_id` bigint NULL DEFAULT NULL COMMENT '关联的原始对话ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识点标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '摘要',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '详细内容',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态：0=待确认，1=已确认入库，2=已丢弃',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  `need_review` int NULL DEFAULT 0 COMMENT '是否需要复习（0=不需要，1=需要）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '待确认知识点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for points_log
-- ----------------------------
DROP TABLE IF EXISTS `points_log`;
CREATE TABLE `points_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `points` int NOT NULL COMMENT '积分变动（正数为获得，负数为消耗）',
  `type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '积分类型：review/create/checkin/achievement/streak_bonus/makeup_use',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '积分描述',
  `reference_id` bigint NULL DEFAULT NULL COMMENT '关联ID（如 review_card_id, node_id, achievement_id）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分变动日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for raw_chat_record
-- ----------------------------
DROP TABLE IF EXISTS `raw_chat_record`;
CREATE TABLE `raw_chat_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `platform` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源平台 ChatGPT/DeepSeek/Kimi/Other',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对话内容',
  `source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始链接',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `processed` tinyint NULL DEFAULT 0 COMMENT 'processed: 0-not processed, 1-processed',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_platform`(`platform` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_rcr_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '原始对话记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_conclusion
-- ----------------------------
DROP TABLE IF EXISTS `research_conclusion`;
CREATE TABLE `research_conclusion`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `task_id` bigint NULL DEFAULT NULL COMMENT '关联任务 ID',
  `statement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结论陈述',
  `confidence` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'medium' COMMENT 'high/medium/low/speculation',
  `supporting_sources` json NULL COMMENT '支持来源 [{sourceId,quote,relevance}]',
  `conflicting_sources` json NULL COMMENT '冲突来源 [{sourceId,quote}]',
  `is_key_finding` tinyint NULL DEFAULT 0 COMMENT '是否为关键发现',
  `is_controversial` tinyint NULL DEFAULT 0 COMMENT '是否存在冲突信息',
  `critic_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Critic Agent 验证备注',
  `version` int NOT NULL DEFAULT 1 COMMENT '结论版本号',
  `previous_version_id` bigint NULL DEFAULT NULL COMMENT '上一版本 conclusion_id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rcon_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rcon_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_rcon_confidence`(`confidence` ASC) USING BTREE,
  INDEX `idx_rcon_is_key_finding`(`is_key_finding` ASC) USING BTREE,
  INDEX `idx_rcon_project_confidence`(`project_id` ASC, `confidence` ASC) USING BTREE,
  CONSTRAINT `research_conclusion_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `research_conclusion_ibfk_2` FOREIGN KEY (`task_id`) REFERENCES `research_task` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究结论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_finding
-- ----------------------------
DROP TABLE IF EXISTS `research_finding`;
CREATE TABLE `research_finding`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `task_id` bigint NULL DEFAULT NULL COMMENT '关联任务 ID',
  `agent_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产生该发现的 Agent',
  `statement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发现内容',
  `category` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类：fact/insight/question/contradiction/gap',
  `source_ids` json NULL COMMENT '支撑来源 ID 列表 [1,2,3]',
  `confidence` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'medium' COMMENT '可信度：high/medium/low/speculation',
  `is_promoted` tinyint NULL DEFAULT 0 COMMENT '是否已升级为 conclusion',
  `promoted_to_id` bigint NULL DEFAULT NULL COMMENT '升级后的 conclusion_id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rfnd_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rfnd_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_rfnd_agent_name`(`agent_name` ASC) USING BTREE,
  INDEX `idx_rfnd_category`(`category` ASC) USING BTREE,
  INDEX `idx_rfnd_is_promoted`(`is_promoted` ASC) USING BTREE,
  CONSTRAINT `research_finding_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `research_finding_ibfk_2` FOREIGN KEY (`task_id`) REFERENCES `research_task` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究中间发现表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_history
-- ----------------------------
DROP TABLE IF EXISTS `research_history`;
CREATE TABLE `research_history`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `current_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `target_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `depth` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '深度（仅学习报告）',
  `user_knowledge` json NULL,
  `knowledge_count` int NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` int NOT NULL DEFAULT 0,
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_rh_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_knowledge_candidate
-- ----------------------------
DROP TABLE IF EXISTS `research_knowledge_candidate`;
CREATE TABLE `research_knowledge_candidate`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `source_ids` json NULL COMMENT '支撑来源 ID 列表',
  `conclusion_ids` json NULL COMMENT '支撑结论 ID 列表',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '建议标题',
  `content_md` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '建议正文（Markdown）',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '摘要',
  `tags` json NULL COMMENT '建议标签 [\"tag1\",\"tag2\"]',
  `importance` tinyint NULL DEFAULT 3 COMMENT '建议重要程度 1-5',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/MODIFIED/WRITTEN',
  `written_node_id` bigint NULL DEFAULT NULL COMMENT '写入后的 knowledge_node.id',
  `user_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用户修改意见',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '用户审核时间',
  `written_at` datetime NULL DEFAULT NULL COMMENT '写入知识库时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rkc_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rkc_status`(`status` ASC) USING BTREE,
  INDEX `idx_rkc_written_node_id`(`written_node_id` ASC) USING BTREE,
  CONSTRAINT `research_knowledge_candidate_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究知识候选表（待用户确认）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_memory
-- ----------------------------
DROP TABLE IF EXISTS `research_memory`;
CREATE TABLE `research_memory`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `memory_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记忆键（唯一标识）',
  `memory_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：knowledges_state/gap_found/search_result/user_preference/decision',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记忆内容（Markdown 文本）',
  `last_accessed_at` datetime NULL DEFAULT NULL COMMENT '上次访问时间',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '过期时间（NULL=永不过期）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rmem_project_memory`(`project_id` ASC, `memory_key` ASC) USING BTREE,
  INDEX `idx_rmem_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rmem_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_rmem_expires_at`(`expires_at` ASC) USING BTREE,
  CONSTRAINT `research_memory_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 144 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent 长期记忆表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_plan
-- ----------------------------
DROP TABLE IF EXISTS `research_plan`;
CREATE TABLE `research_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `version` int NOT NULL DEFAULT 1 COMMENT '计划版本号',
  `complexity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '复杂度评估',
  `agent_chain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Agent 执行链路（逗号分隔）',
  `tasks_json` json NOT NULL COMMENT 'Task 列表 [{title,description,requiresExternalSearch,dependsOn}]',
  `rationale` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Planner 推理过程',
  `estimated_tokens` int NULL DEFAULT NULL COMMENT '预估 token 消耗',
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建者：PLANNER_AGENT/USER',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rplan_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rplan_project_version`(`project_id` ASC, `version` ASC) USING BTREE,
  CONSTRAINT `research_plan_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究计划表（Planner 输出）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_project
-- ----------------------------
DROP TABLE IF EXISTS `research_project`;
CREATE TABLE `research_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作空间 ID（NULL=个人空间）',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '研究标题',
  `goal` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '研究目标描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PLANNING/RESEARCHING/REVIEWING/SYNTHESIZING/COMPLETED/ARCHIVED/FAILED/PAUSED',
  `complexity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '复杂度：SIMPLE/STANDARD/DEEP',
  `agent_workflow` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际执行的 Agent 链路（逗号分隔）',
  `max_iterations` int NOT NULL DEFAULT 3 COMMENT '最大 Agent 迭代轮数',
  `current_iteration` int NOT NULL DEFAULT 0 COMMENT '当前迭代轮数',
  `plan_json` json NULL COMMENT 'Research Plan 结构化数据（Planner 输出）',
  `result_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '研究结论摘要',
  `result_report` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完整研究报告（Markdown）',
  `context_snapshot` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AgentContext 序列化快照（用于暂停恢复）',
  `idempotency_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '幂等键（执行操作去重）',
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `started_at` datetime NULL DEFAULT NULL COMMENT '首次执行时间',
  `paused_at` datetime NULL DEFAULT NULL COMMENT '最近暂停时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rp_idempotency`(`idempotency_key` ASC) USING BTREE,
  INDEX `idx_rp_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_rp_workspace_id`(`workspace_id` ASC) USING BTREE,
  INDEX `idx_rp_status`(`status` ASC) USING BTREE,
  INDEX `idx_rp_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_rp_user_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_report
-- ----------------------------
DROP TABLE IF EXISTS `research_report`;
CREATE TABLE `research_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `version` int NOT NULL DEFAULT 1 COMMENT '报告版本号',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '报告标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报告摘要',
  `content_md` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报告正文（Markdown）',
  `research_questions` json NULL COMMENT '研究问题列表',
  `key_findings` json NULL COMMENT '关键发现 [{statement,confidence,sourceIds}]',
  `knowledge_gaps` json NULL COMMENT '发现的知识缺口 [{topic,description,priority}]',
  `new_knowledge_ids` json NULL COMMENT '新写入的 knowledge_node ID 列表',
  `new_relation_ids` json NULL COMMENT '新写入的 knowledge_relation ID 列表',
  `source_count` int NULL DEFAULT 0 COMMENT '引用来源总数',
  `conclusion_count` int NULL DEFAULT 0 COMMENT '结论总数',
  `token_usage_total` json NULL COMMENT '总 Token 消耗 {\"prompt\":N,\"completion\":N}',
  `duration_total_ms` bigint NULL DEFAULT NULL COMMENT '总耗时（毫秒）',
  `generated_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'SYNTHESIZER_AGENT' COMMENT '生成方',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rrpt_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rrpt_project_version`(`project_id` ASC, `version` ASC) USING BTREE,
  CONSTRAINT `research_report_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究报告表（最终输出）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_source
-- ----------------------------
DROP TABLE IF EXISTS `research_source`;
CREATE TABLE `research_source`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `task_id` bigint NULL DEFAULT NULL COMMENT '关联任务 ID',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源标题',
  `url` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源 URL',
  `source_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型：web_search/official_doc/paper/github/article/internal',
  `snippet` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容摘要',
  `full_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完整内容',
  `relevance_score` decimal(5, 4) NULL DEFAULT NULL COMMENT '与研究的关联度 0-1',
  `reliability` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'unverified' COMMENT '可靠性：high/medium/low/unverified',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容 SHA-256 哈希（去重）',
  `fetch_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'success' COMMENT '抓取状态：success/failed/timeout/skipped',
  `fetched_at` datetime NULL DEFAULT NULL COMMENT '抓取时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rsrc_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rsrc_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_rsrc_source_type`(`source_type` ASC) USING BTREE,
  INDEX `idx_rsrc_reliability`(`reliability` ASC) USING BTREE,
  INDEX `idx_rsrc_content_hash`(`content_hash` ASC) USING BTREE,
  FULLTEXT INDEX `ft_rsrc_content`(`title`, `snippet`),
  CONSTRAINT `research_source_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `research_source_ibfk_2` FOREIGN KEY (`task_id`) REFERENCES `research_task` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 91 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究来源表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_step
-- ----------------------------
DROP TABLE IF EXISTS `research_step`;
CREATE TABLE `research_step`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` int NULL DEFAULT NULL,
  `execution_id` bigint NULL DEFAULT NULL COMMENT '所属执行会话 ID',
  `agent_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Agent 标识',
  `step_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤类型：THINKING/TOOL_CALL/TOOL_RESULT/LLM_CALL/PROCESSING',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '步骤描述（前端展示）',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '步骤内容',
  `tool_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Tool 名称',
  `tool_input` json NULL COMMENT 'Tool 入参',
  `tool_output` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Tool 结果',
  `token_usage` json NULL COMMENT 'Token 消耗 {\"prompt\":N,\"completion\":N}',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/FAILED/SKIPPED',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `sort_order` int NULL DEFAULT 0 COMMENT '步骤序号',
  `duration_ms` int NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rstep_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_rstep_execution_id`(`execution_id` ASC) USING BTREE,
  INDEX `idx_rstep_agent_name`(`agent_name` ASC) USING BTREE,
  INDEX `idx_rstep_status`(`status` ASC) USING BTREE,
  INDEX `idx_rstep_task_agent`(`task_id` ASC, `agent_name` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究步骤表（Agent 执行日志）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for research_task
-- ----------------------------
DROP TABLE IF EXISTS `research_task`;
CREATE TABLE `research_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT '所属项目 ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '所属计划 ID',
  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务描述',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '该任务要回答的研究问题',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED/SKIPPED/WAITING_USER',
  `depends_on` bigint NULL DEFAULT NULL COMMENT '依赖的前置 task_id',
  `async_task_id` bigint NULL DEFAULT NULL COMMENT '关联 async_task 表 ID',
  `requires_external_search` tinyint NULL DEFAULT 0 COMMENT '是否需要外部搜索',
  `result_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务结果摘要',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序序号',
  `started_at` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rtask_project_id`(`project_id` ASC) USING BTREE,
  INDEX `idx_rtask_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_rtask_status`(`status` ASC) USING BTREE,
  INDEX `idx_rtask_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_rtask_project_status`(`project_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `research_task_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `research_project` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `research_task_ibfk_2` FOREIGN KEY (`plan_id`) REFERENCES `research_plan` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 76 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '研究任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for review_card
-- ----------------------------
DROP TABLE IF EXISTS `review_card`;
CREATE TABLE `review_card`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `card_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'simple',
  `difficulty` int NOT NULL DEFAULT 1,
  `review_count` int NOT NULL DEFAULT 0,
  `correct_count` int NOT NULL DEFAULT 0,
  `incorrect_count` int NOT NULL DEFAULT 0,
  `mastery_level` int NOT NULL DEFAULT 0,
  `memory_strength` decimal(5, 4) NOT NULL DEFAULT 0.0000,
  `last_review_time` datetime NULL DEFAULT NULL,
  `next_review_time` datetime NULL DEFAULT NULL,
  `status` int NOT NULL DEFAULT 0,
  `ai_generated` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'false',
  `generation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'auto' COMMENT '????????uto-???????????????, manual-???????????????',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` int NOT NULL DEFAULT 0,
  `is_restored` tinyint(1) NULL DEFAULT 0 COMMENT '是否被恢复过：0-否，1-是',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_next_review_time`(`next_review_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_generation_type`(`generation_type` ASC) USING BTREE,
  INDEX `idx_status_generation_type`(`status` ASC, `generation_type` ASC) USING BTREE,
  INDEX `idx_rc_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2126 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for review_card_pool
-- ----------------------------
DROP TABLE IF EXISTS `review_card_pool`;
CREATE TABLE `review_card_pool`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NOT NULL COMMENT '关联知识点ID',
  `workspace_id` bigint NOT NULL COMMENT '所属工作区ID',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '题目内容',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正确答案',
  `card_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'choice' COMMENT '题型: choice/fill/essay/judge',
  `difficulty` int NOT NULL DEFAULT 1 COMMENT '初始难度 1-5',
  `generation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'auto' COMMENT '生成方式: auto/manual',
  `create_user_id` bigint NOT NULL COMMENT '创建者（操作确认入库的人）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pool_ws`(`workspace_id` ASC) USING BTREE,
  INDEX `idx_pool_node`(`node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for review_log
-- ----------------------------
DROP TABLE IF EXISTS `review_log`;
CREATE TABLE `review_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `node_id` bigint NOT NULL COMMENT '知识节点ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '复习结果 easy/hard/forgot/completed',
  `duration` int NULL DEFAULT NULL COMMENT '复习时长（秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_result`(`result` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_rl_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '敏感词',
  `created_at` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `word`(`word` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for share_link
-- ----------------------------
DROP TABLE IF EXISTS `share_link`;
CREATE TABLE `share_link`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `node_id` bigint NOT NULL COMMENT '知识节点ID',
  `owner_id` bigint NOT NULL COMMENT '分享者用户ID',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '随机分享令牌（SHA-256生成，64字符，不可猜测）',
  `expire_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'permanent' COMMENT '有效期类型：permanent/7d/24h',
  `expires_at` datetime NULL DEFAULT NULL COMMENT '具体过期时间（permanent时为null）',
  `access_count` int NULL DEFAULT 0 COMMENT '累计访问次数',
  `is_revoked` tinyint NULL DEFAULT 0 COMMENT '是否已撤销（0-未撤销，1-已撤销）',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `token`(`token` ASC) USING BTREE,
  INDEX `idx_node_id`(`node_id` ASC) USING BTREE,
  INDEX `idx_owner_id`(`owner_id` ASC) USING BTREE,
  INDEX `idx_token`(`token` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识节点分享链接' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_bookmark
-- ----------------------------
DROP TABLE IF EXISTS `square_bookmark`;
CREATE TABLE `square_bookmark`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `created_at` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_comment
-- ----------------------------
DROP TABLE IF EXISTS `square_comment`;
CREATE TABLE `square_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论者用户ID',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `created_at` datetime NOT NULL COMMENT '评论时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_like
-- ----------------------------
DROP TABLE IF EXISTS `square_like`;
CREATE TABLE `square_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '点赞用户ID',
  `created_at` datetime NOT NULL COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点赞记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_post
-- ----------------------------
DROP TABLE IF EXISTS `square_post`;
CREATE TABLE `square_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `node_id` bigint NOT NULL COMMENT '关联的知识节点ID',
  `author_id` bigint NOT NULL COMMENT '发布者用户ID',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'global' COMMENT '发布范围：global/workspace',
  `workspace_id` bigint NULL DEFAULT NULL COMMENT '工作区ID（scope=workspace时使用）',
  `recommend_text` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '推荐语',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数（冗余计数器）',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数（冗余计数器）',
  `bookmark_count` int NULL DEFAULT 0 COMMENT '收藏数（冗余计数器）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'published' COMMENT '状态：published/removed',
  `created_at` datetime NOT NULL COMMENT '发布时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_like_count`(`like_count` ASC) USING BTREE,
  INDEX `idx_scope_workspace`(`scope` ASC, `workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '广场帖子' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_post_node
-- ----------------------------
DROP TABLE IF EXISTS `square_post_node`;
CREATE TABLE `square_post_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '广场帖子ID',
  `node_id` bigint NOT NULL COMMENT '知识节点ID',
  `position` int NOT NULL COMMENT '节点在合集中的排序位置，从0开始',
  `created_at` datetime NOT NULL COMMENT '关联创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_square_post_node`(`post_id` ASC, `node_id` ASC) USING BTREE,
  INDEX `idx_square_post_node_post_position`(`post_id` ASC, `position` ASC) USING BTREE,
  INDEX `idx_square_post_node_node_id`(`node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '广场帖子知识节点关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for square_report
-- ----------------------------
DROP TABLE IF EXISTS `square_report`;
CREATE TABLE `square_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '被举报帖子ID',
  `reporter_id` bigint NOT NULL COMMENT '举报人用户ID',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '举报原因',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending/ignored/removed',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID（admin）',
  `handle_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `created_at` datetime NOT NULL COMMENT '举报时间',
  `handled_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '举报记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人简介',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '???URL',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API Key',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'user' COMMENT '平台角色：super_admin/user',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_achievement
-- ----------------------------
DROP TABLE IF EXISTS `user_achievement`;
CREATE TABLE `user_achievement`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `achievement_id` bigint NOT NULL COMMENT '成就ID',
  `unlocked_at` datetime NOT NULL COMMENT '解锁时间',
  `notified` tinyint NOT NULL DEFAULT 0 COMMENT '是否已通知（0-未通知，1-已通知）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_achievement`(`user_id` ASC, `achievement_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_unlocked_at`(`unlocked_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户成就记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_ai_config
-- ----------------------------
DROP TABLE IF EXISTS `user_ai_config`;
CREATE TABLE `user_ai_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `scenario_code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景代码：chat/extraction/question_gen/embedding/research',
  `provider_id` bigint NOT NULL COMMENT '关联ai_provider.id',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户选择的模型（预设或自定义）',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AES-256-GCM加密存储的用户API Key',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_scenario`(`user_id` ASC, `scenario_code` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_provider_id`(`provider_id` ASC) USING BTREE,
  INDEX `idx_scenario_code`(`scenario_code` ASC) USING BTREE,
  CONSTRAINT `user_ai_config_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `ai_provider` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户AI场景配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_ai_provider_key
-- ----------------------------
DROP TABLE IF EXISTS `user_ai_provider_key`;
CREATE TABLE `user_ai_provider_key`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `provider_id` bigint NOT NULL COMMENT '关联ai_provider.id',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AES-256-GCM加密存储的API Key',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_provider`(`user_id` ASC, `provider_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_provider_id`(`provider_id` ASC) USING BTREE,
  CONSTRAINT `user_ai_provider_key_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `ai_provider` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户服务商全局Key表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_block
-- ----------------------------
DROP TABLE IF EXISTS `user_block`;
CREATE TABLE `user_block`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '拉黑关系ID',
  `blocker_id` bigint NOT NULL COMMENT '拉黑发起者用户ID',
  `blocked_id` bigint NOT NULL COMMENT '被拉黑用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_block`(`blocker_id` ASC, `blocked_id` ASC) USING BTREE,
  INDEX `idx_user_block_blocked`(`blocked_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_block_blocker`(`blocker_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户拉黑关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_follow
-- ----------------------------
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注关系ID',
  `follower_id` bigint NOT NULL COMMENT '关注者用户ID',
  `followed_id` bigint NOT NULL COMMENT '被关注用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_follow`(`follower_id` ASC, `followed_id` ASC) USING BTREE,
  INDEX `idx_user_follow_followed`(`followed_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_follow_follower`(`follower_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户单向关注关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_gamification
-- ----------------------------
DROP TABLE IF EXISTS `user_gamification`;
CREATE TABLE `user_gamification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_points` bigint NOT NULL DEFAULT 0 COMMENT '累计总积分',
  `current_points` bigint NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  `level` int NOT NULL DEFAULT 1 COMMENT '用户等级（1-50）',
  `experience` bigint NOT NULL DEFAULT 0 COMMENT '当前等级经验值',
  `experience_to_next_level` bigint NOT NULL DEFAULT 250 COMMENT '升级所需经验值',
  `current_streak` int NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
  `max_streak` int NOT NULL DEFAULT 0 COMMENT '历史最长连续签到天数',
  `last_check_in_date` date NULL DEFAULT NULL COMMENT '最后签到日期',
  `last_review_date` date NULL DEFAULT NULL COMMENT '最后复习日期',
  `makeup_cards_remaining` int NOT NULL DEFAULT 2 COMMENT '剩余补签卡数量',
  `makeup_cards_used_this_month` int NOT NULL DEFAULT 0 COMMENT '本月已使用补签卡数',
  `total_review_count` int NOT NULL DEFAULT 0 COMMENT '累计复习次数',
  `total_correct_count` int NOT NULL DEFAULT 0 COMMENT '累计正确次数',
  `total_node_count` int NOT NULL DEFAULT 0 COMMENT '累计创建知识节点数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_total_points`(`total_points` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_current_streak`(`current_streak` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户游戏化数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_oauth_identity
-- ----------------------------
DROP TABLE IF EXISTS `user_oauth_identity`;
CREATE TABLE `user_oauth_identity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '第三方身份映射ID',
  `user_id` bigint NOT NULL COMMENT '平台用户ID',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '身份提供方，例如 wechat',
  `provider_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提供方用户标识（openid等）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_oauth_provider_user`(`provider` ASC, `provider_user_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_oauth_user_provider`(`user_id` ASC, `provider` ASC) USING BTREE,
  INDEX `idx_oauth_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '第三方登录身份映射' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_review_card
-- ----------------------------
DROP TABLE IF EXISTS `user_review_card`;
CREATE TABLE `user_review_card`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pool_id` bigint NULL DEFAULT NULL COMMENT 'FK→review_card_pool.id，NULL=无池子模板的个人卡片',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `workspace_id` bigint NOT NULL COMMENT '工作区ID',
  `review_count` int NOT NULL DEFAULT 0 COMMENT '复习次数',
  `correct_count` int NOT NULL DEFAULT 0 COMMENT '正确次数',
  `incorrect_count` int NOT NULL DEFAULT 0 COMMENT '错误次数',
  `mastery_level` int NOT NULL DEFAULT 0 COMMENT '掌握程度 0-5',
  `memory_strength` decimal(5, 4) NOT NULL DEFAULT 0.0000 COMMENT '记忆强度 0-1',
  `last_review_time` datetime NULL DEFAULT NULL COMMENT '上次复习时间',
  `next_review_time` datetime NULL DEFAULT NULL COMMENT '下次复习时间',
  `status` int NOT NULL DEFAULT 0 COMMENT '0=待复习, 1=已掌握',
  `is_archived` tinyint NULL DEFAULT 0 COMMENT '是否归档（重新加入后旧副本标1）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_urc_pool`(`pool_id` ASC) USING BTREE,
  INDEX `idx_urc_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_urc_ws`(`workspace_id` ASC) USING BTREE,
  INDEX `idx_urc_next_review`(`next_review_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 350 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_review_preference
-- ----------------------------
DROP TABLE IF EXISTS `user_review_preference`;
CREATE TABLE `user_review_preference`  (
  `user_id` bigint NOT NULL COMMENT '鐢ㄦ埛ID锛屽悓鏃朵綔涓哄亸濂借?褰曚富閿',
  `interval_days_json` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鑷?畾涔夊?涔犻棿闅斿ぉ鏁扮殑JSON鏁扮粍',
  `review_email_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '鏄?惁寮?惎澶嶄範鎻愰啋閭?欢锛?鍚︼紝1鏄',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛澶嶄範鍋忓ソ' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workspace
-- ----------------------------
DROP TABLE IF EXISTS `workspace`;
CREATE TABLE `workspace`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作区ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作区名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '工作区描述',
  `owner_id` bigint NOT NULL COMMENT '创建者用户ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_owner`(`owner_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作区/租户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for workspace_member
-- ----------------------------
DROP TABLE IF EXISTS `workspace_member`;
CREATE TABLE `workspace_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员记录ID',
  `workspace_id` bigint NOT NULL COMMENT '工作区ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色：owner/admin/editor/viewer',
  `joined_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'accepted' COMMENT '成员状态：pending-待确认，accepted-已确认',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ws_user`(`workspace_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作区成员' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
