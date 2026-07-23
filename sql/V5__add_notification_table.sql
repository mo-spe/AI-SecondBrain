-- 知识广场 V2.1 — 通知系统 + 工作区广场增强
-- 日期：2026-07-23

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '接收通知的用户ID',
    type VARCHAR(30) NOT NULL COMMENT '通知类型：like/comment/report_result',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) COMMENT '通知内容',
    target_type VARCHAR(30) COMMENT '关联目标类型：post',
    target_id BIGINT COMMENT '关联目标ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME NOT NULL COMMENT '通知时间',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';
