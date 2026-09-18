-- 知识广场评论互动：楼中楼回复 + 评论点赞
--
-- 问题：原 square_comment 仅支持一级评论，且评论无法被点赞，用户间缺少互动。
--
-- 方案：
--   1. square_comment 增加 parent_id（顶层评论为 NULL，楼中楼回复统一挂在其所属顶层评论下）、
--      reply_to_user_id（被回复人，用于展示“回复 @昵称”）、like_count（评论点赞数）。
--   2. 新增 square_comment_like 表记录评论点赞（唯一约束保证一人一条）。
--
-- 用法：mysql -h 127.0.0.1 -P 3307 -u root -p second_brain < V14__add_square_comment_interaction.sql
-- 字段已存在时报 Duplicate column name 属正常，可忽略（说明 second_brain.sql 已含这些列）。

USE second_brain;

ALTER TABLE square_comment
    ADD COLUMN parent_id bigint NULL DEFAULT NULL
        COMMENT '父评论ID，顶层评论为NULL；楼中楼回复统一挂在其所属顶层评论下' AFTER content,
    ADD COLUMN reply_to_user_id bigint NULL DEFAULT NULL
        COMMENT '被回复人用户ID，用于展示“回复 @昵称”，顶层评论为NULL' AFTER parent_id,
    ADD COLUMN like_count int NOT NULL DEFAULT 0
        COMMENT '评论点赞数，从 square_comment_like 重算' AFTER reply_to_user_id,
    ADD INDEX idx_square_comment_parent (parent_id);

CREATE TABLE IF NOT EXISTS square_comment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL COMMENT '点赞时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_square_comment_like_comment (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录';
