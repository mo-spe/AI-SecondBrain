-- 知识广场 V2.0 MVP — 数据库迁移
-- 日期：2026-07-23
-- 说明：广场帖子、点赞、评论、收藏、举报、敏感词库

CREATE TABLE IF NOT EXISTS square_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    node_id BIGINT NOT NULL COMMENT '关联的知识节点ID',
    author_id BIGINT NOT NULL COMMENT '发布者用户ID',
    scope VARCHAR(20) NOT NULL DEFAULT 'global' COMMENT '发布范围：global/workspace',
    workspace_id BIGINT NULL COMMENT '工作区ID（scope=workspace时使用）',
    recommend_text VARCHAR(200) COMMENT '推荐语',
    like_count INT DEFAULT 0 COMMENT '点赞数（冗余计数器）',
    comment_count INT DEFAULT 0 COMMENT '评论数（冗余计数器）',
    bookmark_count INT DEFAULT 0 COMMENT '收藏数（冗余计数器）',
    status VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '状态：published/removed',
    created_at DATETIME NOT NULL COMMENT '发布时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_like_count (like_count),
    INDEX idx_scope_workspace (scope, workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场帖子';

CREATE TABLE IF NOT EXISTS square_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME NOT NULL COMMENT '点赞时间',
    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录';

CREATE TABLE IF NOT EXISTS square_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '评论者用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    created_at DATETIME NOT NULL COMMENT '评论时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

CREATE TABLE IF NOT EXISTS square_bookmark (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '收藏用户ID',
    created_at DATETIME NOT NULL COMMENT '收藏时间',
    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏';

CREATE TABLE IF NOT EXISTS square_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '被举报帖子ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人用户ID',
    reason VARCHAR(500) NOT NULL COMMENT '举报原因',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending/ignored/removed',
    handler_id BIGINT NULL COMMENT '处理人ID（admin）',
    handle_note VARCHAR(500) NULL COMMENT '处理备注',
    created_at DATETIME NOT NULL COMMENT '举报时间',
    handled_at DATETIME NULL COMMENT '处理时间',
    INDEX idx_post_id (post_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报记录';

CREATE TABLE IF NOT EXISTS sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    word VARCHAR(100) NOT NULL UNIQUE COMMENT '敏感词',
    created_at DATETIME NOT NULL COMMENT '添加时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库';
