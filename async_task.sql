CREATE TABLE IF NOT EXISTS async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'task ID',
    task_number VARCHAR(64) NOT NULL UNIQUE COMMENT 'task number',
    user_id BIGINT NOT NULL COMMENT 'user ID',
    task_type VARCHAR(50) NOT NULL COMMENT 'task type',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'task status',
    progress INT DEFAULT 0 COMMENT 'progress',
    parameters TEXT COMMENT 'task parameters',
    result TEXT COMMENT 'task result',
    error_message TEXT COMMENT 'error message',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    start_time DATETIME COMMENT 'start time',
    complete_time DATETIME COMMENT 'complete time',
    deleted INT NOT NULL DEFAULT 0 COMMENT 'deleted',
    INDEX idx_user_id (user_id),
    INDEX idx_task_number (task_number),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='async task table';