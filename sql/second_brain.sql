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

 Date: 24/07/2026 09:53:07
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
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成就定义表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天会话表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '每日签到记录表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '编辑锁' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 204 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑鍚戦噺琛' ROW_FORMAT = Dynamic;

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
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_chat_record_id`(`chat_record_id` ASC) USING BTREE,
  INDEX `idx_next_review_time`(`next_review_time` ASC) USING BTREE,
  INDEX `idx_importance`(`importance` ASC) USING BTREE,
  INDEX `idx_mastery_level`(`mastery_level` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_kn_ws`(`workspace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 341 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识节点表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识节点标签关联表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 132 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑鍏崇郴琛' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识节点版本历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for knowledge_tag
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_tag`;
CREATE TABLE `knowledge_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `tag_color` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签颜色',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_tag_name`(`tag_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识标签表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '排行榜快照表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分变动日志表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '原始对话记录表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '复习记录表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点赞记录' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户成就记录表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户游戏化数据表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作区成员' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
