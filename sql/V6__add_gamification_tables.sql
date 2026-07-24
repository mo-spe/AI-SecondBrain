-- V6__add_gamification_tables.sql
-- 学习游戏化与参与感 — 数据库迁移
-- 创建 6 张游戏化相关表 + 18 个成就种子数据

-- ==================== 用户游戏化状态 ====================
CREATE TABLE IF NOT EXISTS user_gamification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_points BIGINT NOT NULL DEFAULT 0 COMMENT '累计总积分',
    current_points BIGINT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    level INT NOT NULL DEFAULT 1 COMMENT '用户等级（1-50）',
    experience BIGINT NOT NULL DEFAULT 0 COMMENT '当前等级经验值',
    experience_to_next_level BIGINT NOT NULL DEFAULT 250 COMMENT '升级所需经验值',
    current_streak INT NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
    max_streak INT NOT NULL DEFAULT 0 COMMENT '历史最长连续签到天数',
    last_check_in_date DATE COMMENT '最后签到日期',
    last_review_date DATE COMMENT '最后复习日期',
    makeup_cards_remaining INT NOT NULL DEFAULT 2 COMMENT '剩余补签卡数量',
    makeup_cards_used_this_month INT NOT NULL DEFAULT 0 COMMENT '本月已使用补签卡数',
    total_review_count INT NOT NULL DEFAULT 0 COMMENT '累计复习次数',
    total_correct_count INT NOT NULL DEFAULT 0 COMMENT '累计正确次数',
    total_node_count INT NOT NULL DEFAULT 0 COMMENT '累计创建知识节点数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_total_points (total_points),
    INDEX idx_level (level),
    INDEX idx_current_streak (current_streak)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户游戏化数据表';

-- ==================== 每日签到记录 ====================
CREATE TABLE IF NOT EXISTS daily_check_in (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    check_in_date DATE NOT NULL COMMENT '签到日期',
    points_earned INT NOT NULL DEFAULT 0 COMMENT '本次签到获得积分',
    streak_bonus_multiplier DECIMAL(3,1) NOT NULL DEFAULT 1.0 COMMENT '连续签到加成倍数',
    is_makeup TINYINT NOT NULL DEFAULT 0 COMMENT '是否为补签（0-正常签到，1-补签）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
    UNIQUE KEY uk_user_date (user_id, check_in_date),
    INDEX idx_user_id (user_id),
    INDEX idx_check_in_date (check_in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到记录表';

-- ==================== 成就定义表（种子数据） ====================
CREATE TABLE IF NOT EXISTS achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    code VARCHAR(50) NOT NULL COMMENT '成就代码（如 first_review）',
    name VARCHAR(100) NOT NULL COMMENT '成就名称',
    description VARCHAR(500) NOT NULL COMMENT '成就描述',
    icon VARCHAR(100) COMMENT '成就图标（Element Plus icon name）',
    category VARCHAR(30) NOT NULL COMMENT '成就分类：review/knowledge/streak/accuracy/mastery',
    tier VARCHAR(20) NOT NULL COMMENT '等级：bronze/silver/gold/platinum',
    trigger_type VARCHAR(50) NOT NULL COMMENT '触发类型：review_count/knowledge_count/streak_days/accuracy_rate/mastery_first',
    trigger_value INT NOT NULL COMMENT '触发阈值',
    points_reward INT NOT NULL DEFAULT 0 COMMENT '解锁奖励积分',
    makeup_card_reward INT NOT NULL DEFAULT 0 COMMENT '解锁奖励补签卡数',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_code (code),
    INDEX idx_category (category),
    INDEX idx_tier (tier),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

-- ==================== 用户成就记录 ====================
CREATE TABLE IF NOT EXISTS user_achievement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    achievement_id BIGINT NOT NULL COMMENT '成就ID',
    unlocked_at DATETIME NOT NULL COMMENT '解锁时间',
    notified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已通知（0-未通知，1-已通知）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    INDEX idx_user_id (user_id),
    INDEX idx_unlocked_at (unlocked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就记录表';

-- 兜底：如果表已存在但没有 create_time 列，则补充
ALTER TABLE user_achievement ADD COLUMN IF NOT EXISTS create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- ==================== 积分变动日志 ====================
CREATE TABLE IF NOT EXISTS points_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '积分变动（正数为获得，负数为消耗）',
    type VARCHAR(30) NOT NULL COMMENT '积分类型：review/create/checkin/achievement/streak_bonus/makeup_use',
    description VARCHAR(200) COMMENT '积分描述',
    reference_id BIGINT COMMENT '关联ID（如 review_card_id, node_id, achievement_id）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志表';

-- ==================== 排行榜快照 ====================
CREATE TABLE IF NOT EXISTS leaderboard_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    period VARCHAR(10) NOT NULL COMMENT '排行榜周期：daily/weekly/monthly/all',
    domain VARCHAR(50) NOT NULL DEFAULT 'all' COMMENT '领域（标签名称或 all）',
    rank_position INT NOT NULL COMMENT '排名',
    score BIGINT NOT NULL COMMENT '当期综合得分',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_period_domain_date (user_id, period, domain, snapshot_date),
    INDEX idx_period_domain_rank (period, domain, snapshot_date, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜快照表';

-- ==================== 成就种子数据 ====================
INSERT INTO achievement (code, name, description, icon, category, tier, trigger_type, trigger_value, points_reward, makeup_card_reward, sort_order) VALUES
-- 复习类成就
('first_review',  '初出茅庐', '完成第1次复习',             'Medal',       'review',    'bronze',   'review_count',   1,    50,   0, 1),
('review_10',    '温故知新', '累计完成10次复习',           'Medal',       'review',    'bronze',   'review_count',   10,   100,  0, 2),
('review_50',    '学而不厌', '累计完成50次复习',           'Trophy',      'review',    'silver',   'review_count',   50,   200,  0, 3),
('review_100',   '博览群书', '累计完成100次复习',          'Trophy',      'review',    'gold',     'review_count',   100,  500,  1, 4),
('review_500',   '学问思辨', '累计完成500次复习',          'Trophy',      'review',    'gold',     'review_count',   500,  1000, 2, 5),
('review_1000',  '博闻强识', '累计完成1000次复习',         'Trophy',      'review',    'platinum', 'review_count',   1000, 2000, 3, 6),

-- 知识创建类成就
('first_node',   '知识萌芽', '创建第1个知识节点',         'Document',    'knowledge', 'bronze',   'knowledge_count', 1,    50,   0, 7),
('node_10',      '知识积累', '累计创建10个知识节点',       'Document',    'knowledge', 'bronze',   'knowledge_count', 10,   100,  0, 8),
('node_50',      '学识渊博', '累计创建50个知识节点',       'Collection',  'knowledge', 'silver',   'knowledge_count', 50,   200,  0, 9),
('node_100',     '知识大师', '累计创建100个知识节点',      'Collection',  'knowledge', 'gold',     'knowledge_count', 100,  500,  1, 10),

-- 连续打卡类成就
('streak_3',     '三天打鱼', '连续签到3天',                'Sunny',       'streak',    'bronze',   'streak_days',     3,    50,   0, 11),
('streak_7',     '持之以恒', '连续签到7天',                'Sunny',       'streak',    'silver',   'streak_days',     7,    150,  1, 12),
('streak_30',    '坚如磐石', '连续签到30天',               'Sunrise',     'streak',    'gold',     'streak_days',     30,   500,  2, 13),
('streak_100',   '水滴石穿', '连续签到100天',              'Sunrise',     'streak',    'platinum', 'streak_days',     100,  1500, 5, 14),

-- 正确率类成就
('accuracy_80',  '小有成就', '复习正确率达到80%（至少50次复习）', 'Star',   'accuracy',  'bronze',   'accuracy_rate',   80,   100,  0, 15),
('accuracy_90',  '出类拔萃', '复习正确率达到90%（至少50次复习）', 'Star',   'accuracy',  'silver',   'accuracy_rate',   90,   300,  0, 16),
('accuracy_95',  '炉火纯青', '复习正确率达到95%（至少50次复习）', 'StarFilled', 'accuracy', 'gold',  'accuracy_rate',   95,   500,  1, 17),

-- 精通类成就
('mastery_first', '融会贯通', '精通掌握第1个知识节点（掌握度≥4）', 'MagicStick', 'mastery', 'silver', 'mastery_first', 1, 200, 0, 18);
