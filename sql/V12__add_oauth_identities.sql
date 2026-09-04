-- 移动端第三方登录身份映射。
-- 仅保存不可逆前置平台标识，不保存微信 session_key；用户账户表保持向后兼容。

CREATE TABLE IF NOT EXISTS user_oauth_identity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '第三方身份映射ID',
    user_id BIGINT NOT NULL COMMENT '平台用户ID',
    provider VARCHAR(32) NOT NULL COMMENT '身份提供方，例如 wechat',
    provider_user_id VARCHAR(128) NOT NULL COMMENT '提供方用户标识（openid等）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_oauth_provider_user (provider, provider_user_id),
    UNIQUE KEY uk_oauth_user_provider (user_id, provider),
    INDEX idx_oauth_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='第三方登录身份映射';
