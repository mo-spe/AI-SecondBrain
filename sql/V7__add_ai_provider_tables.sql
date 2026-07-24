-- V7__add_ai_provider_tables.sql
-- 用户级AI模型供应商与模型选择 — 数据库迁移
-- 创建 4 张AI配置相关表 + 9 家服务商种子数据 + 常用模型种子数据

-- ==================== AI服务商定义 ====================
CREATE TABLE IF NOT EXISTS ai_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    code VARCHAR(30) NOT NULL COMMENT '服务商标识（openai/qwen/deepseek/anthropic/gemini/kimi/zhipu/doubao/minimax）',
    name VARCHAR(50) NOT NULL COMMENT '服务商显示名称',
    base_url VARCHAR(255) NOT NULL COMMENT 'API基础地址',
    api_type VARCHAR(30) NOT NULL DEFAULT 'openai_compatible' COMMENT 'API协议类型：openai_compatible/anthropic/gemini',
    logo_url VARCHAR(255) COMMENT '服务商Logo图标地址',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code),
    INDEX idx_is_enabled (is_enabled),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI服务商定义表';

-- ==================== AI模型列表 ====================
CREATE TABLE IF NOT EXISTS ai_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    provider_id BIGINT NOT NULL COMMENT '关联ai_provider.id',
    model_name VARCHAR(100) NOT NULL COMMENT '模型标识（gpt-4o/qwen-plus）',
    display_name VARCHAR(100) NOT NULL COMMENT '显示名称（GPT-4o/通义千问Plus）',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
    supported_scenarios VARCHAR(500) NOT NULL DEFAULT '["chat","extraction","question_gen","research"]' COMMENT '适用场景JSON数组：chat/extraction/question_gen/embedding/research',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_provider_model (provider_id, model_name),
    INDEX idx_provider_id (provider_id),
    INDEX idx_is_enabled (is_enabled),
    FOREIGN KEY (provider_id) REFERENCES ai_provider(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型列表表';

-- ==================== 用户AI场景配置 ====================
CREATE TABLE IF NOT EXISTS user_ai_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scenario_code VARCHAR(30) NOT NULL COMMENT '场景代码：chat/extraction/question_gen/embedding/research',
    provider_id BIGINT NOT NULL COMMENT '关联ai_provider.id',
    model_name VARCHAR(100) NOT NULL COMMENT '用户选择的模型（预设或自定义）',
    api_key VARCHAR(500) COMMENT 'AES-256-GCM加密存储的用户API Key',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_scenario (user_id, scenario_code),
    INDEX idx_user_id (user_id),
    INDEX idx_provider_id (provider_id),
    INDEX idx_scenario_code (scenario_code),
    FOREIGN KEY (provider_id) REFERENCES ai_provider(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户AI场景配置表';

-- ==================== 用户服务商全局Key ====================
CREATE TABLE IF NOT EXISTS user_ai_provider_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    provider_id BIGINT NOT NULL COMMENT '关联ai_provider.id',
    api_key VARCHAR(500) COMMENT 'AES-256-GCM加密存储的API Key',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_provider (user_id, provider_id),
    INDEX idx_user_id (user_id),
    INDEX idx_provider_id (provider_id),
    FOREIGN KEY (provider_id) REFERENCES ai_provider(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户服务商全局Key表';

-- ==================== 服务商种子数据（9家） ====================
INSERT INTO ai_provider (code, name, base_url, api_type, is_enabled, sort_order) VALUES
('openai',     'OpenAI',           'https://api.openai.com',                              'openai_compatible', 1, 1),
('qwen',       '通义千问',          'https://dashscope.aliyuncs.com/compatible-mode',        'openai_compatible', 1, 2),
('deepseek',   'DeepSeek',         'https://api.deepseek.com',                            'openai_compatible', 1, 3),
('anthropic',  'Anthropic',        'https://api.anthropic.com',                           'anthropic',         1, 4),
('gemini',     'Google Gemini',    'https://generativelanguage.googleapis.com',            'gemini',            1, 5),
('kimi',       'Kimi (月之暗面)',    'https://api.moonshot.cn',                             'openai_compatible', 1, 6),
('zhipu',      '智谱AI',            'https://open.bigmodel.cn/api/paas/v4',                'openai_compatible', 1, 7),
('doubao',     '豆包 (字节跳动)',     'https://ark.cn-beijing.volces.com/api/v3',            'openai_compatible', 1, 8),
('minimax',    'MiniMax',          'https://api.minimax.chat/v1',                         'openai_compatible', 1, 9);

-- ==================== 模型种子数据 ====================
-- OpenAI 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'openai'), 'gpt-4o',          'GPT-4o',          1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'openai'), 'gpt-4o-mini',     'GPT-4o Mini',     1, '["chat","extraction","question_gen","research"]', 2),
((SELECT id FROM ai_provider WHERE code = 'openai'), 'gpt-4-turbo',     'GPT-4 Turbo',     1, '["chat","extraction","question_gen","research"]', 3),
((SELECT id FROM ai_provider WHERE code = 'openai'), 'gpt-3.5-turbo',   'GPT-3.5 Turbo',   1, '["chat","extraction","question_gen"]',           4);

-- 通义千问 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'qwen'), 'qwen-plus',         '通义千问 Plus',    1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'qwen'), 'qwen-max',          '通义千问 Max',     1, '["chat","extraction","question_gen","research"]', 2),
((SELECT id FROM ai_provider WHERE code = 'qwen'), 'qwen-turbo',        '通义千问 Turbo',   1, '["chat","extraction","question_gen"]',           3),
((SELECT id FROM ai_provider WHERE code = 'qwen'), 'text-embedding-v2', 'Embedding V2',     1, '["embedding"]',                                   4);

-- DeepSeek 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'deepseek'), 'deepseek-chat',     'DeepSeek Chat',     1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'deepseek'), 'deepseek-reasoner', 'DeepSeek Reasoner', 1, '["chat","research"]',                             2);

-- Anthropic 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'anthropic'), 'claude-sonnet-4-6', 'Claude Sonnet 4.6', 1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'anthropic'), 'claude-opus-4-7',   'Claude Opus 4.7',   1, '["chat","extraction","question_gen","research"]', 2),
((SELECT id FROM ai_provider WHERE code = 'anthropic'), 'claude-haiku-4-5',  'Claude Haiku 4.5',  1, '["chat","extraction","question_gen"]',           3);

-- Google Gemini 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'gemini'), 'gemini-2.5-pro',  'Gemini 2.5 Pro',  1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'gemini'), 'gemini-2.5-flash', 'Gemini 2.5 Flash', 1, '["chat","extraction","question_gen"]',           2);

-- Kimi 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'kimi'), 'moonshot-v1-8k',   'Moonshot v1 8K',   1, '["chat","extraction","question_gen"]',           1),
((SELECT id FROM ai_provider WHERE code = 'kimi'), 'moonshot-v1-32k',  'Moonshot v1 32K',  1, '["chat","extraction","question_gen","research"]', 2),
((SELECT id FROM ai_provider WHERE code = 'kimi'), 'moonshot-v1-128k', 'Moonshot v1 128K', 1, '["chat","research"]',                             3);

-- 智谱AI 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'zhipu'), 'glm-4-plus',  'GLM-4 Plus',  1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'zhipu'), 'glm-4-flash', 'GLM-4 Flash', 1, '["chat","extraction","question_gen"]',           2);

-- 豆包 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'doubao'), 'doubao-pro-32k',  '豆包 Pro 32K',  1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'doubao'), 'doubao-lite-32k', '豆包 Lite 32K', 1, '["chat","extraction","question_gen"]',           2);

-- MiniMax 模型
INSERT INTO ai_model (provider_id, model_name, display_name, is_enabled, supported_scenarios, sort_order) VALUES
((SELECT id FROM ai_provider WHERE code = 'minimax'), 'abab7-chat', 'ABAB7 Chat', 1, '["chat","extraction","question_gen","research"]', 1),
((SELECT id FROM ai_provider WHERE code = 'minimax'), 'abab6.5s-chat', 'ABAB6.5s Chat', 1, '["chat","extraction","question_gen"]', 2);
