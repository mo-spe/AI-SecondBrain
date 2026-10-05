-- 考研个人错题与疑问档案；所有记录由 user_id 隔离，不继承工作区共享可见性。
CREATE TABLE IF NOT EXISTS wrong_question_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NULL,
    image_path VARCHAR(500) NOT NULL,
    ocr_text MEDIUMTEXT NULL,
    user_answer MEDIUMTEXT NULL,
    correct_answer MEDIUMTEXT NULL,
    explanation MEDIUMTEXT NULL,
    subject VARCHAR(120) NULL,
    source_book VARCHAR(255) NULL,
    source_page VARCHAR(80) NULL,
    chapter VARCHAR(255) NULL,
    knowledge_points TEXT NULL,
    error_type VARCHAR(80) NULL,
    user_note TEXT NULL,
    ai_suggestion_json MEDIUMTEXT NULL,
    ai_confidence DECIMAL(5,4) NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'UNSCHEDULED',
    next_review_time DATETIME NULL,
    last_review_time DATETIME NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_wrong_question_user_due (user_id, is_deleted, review_status, next_review_time),
    KEY idx_wrong_question_user_created (user_id, is_deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人错题档案';

CREATE TABLE IF NOT EXISTS wrong_question_review_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wrong_question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    result VARCHAR(20) NOT NULL,
    note TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_wrong_review_record_time (wrong_question_id, create_time),
    KEY idx_wrong_review_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题手动复盘历史';

CREATE TABLE IF NOT EXISTS doubt_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NULL,
    image_path VARCHAR(500) NULL,
    content TEXT NOT NULL,
    source_book VARCHAR(255) NULL,
    source_page VARCHAR(80) NULL,
    chapter VARCHAR(255) NULL,
    doubt_type VARCHAR(80) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_explanation_feedback VARCHAR(20) NULL,
    next_process_time DATETIME NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_doubt_user_due (user_id, is_deleted, status, next_process_time),
    KEY idx_doubt_user_created (user_id, is_deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人疑问档案';

CREATE TABLE IF NOT EXISTS doubt_understanding_revision (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doubt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    understanding_status VARCHAR(20) NOT NULL DEFAULT 'INITIAL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_doubt_revision_history (doubt_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='疑问理解迭代历史';
