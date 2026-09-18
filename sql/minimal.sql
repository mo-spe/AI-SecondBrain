DROP DATABASE IF EXISTS second_brain;
CREATE DATABASE second_brain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE second_brain;

-- ???
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    bio VARCHAR(500),
    avatar VARCHAR(500),
    api_key VARCHAR(255),
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

-- ???????
CREATE TABLE raw_chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform VARCHAR(50),
    content TEXT NOT NULL,
    source_url VARCHAR(500),
    processed TINYINT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

-- ?????
CREATE TABLE knowledge_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_record_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content_md TEXT,
    summary TEXT,
    vector_id VARCHAR(100),
    importance TINYINT NOT NULL DEFAULT 3,
    mastery_level TINYINT NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_review_time DATETIME,
    next_review_time DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0
);

-- AI Provider ???
CREATE TABLE ai_provider (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_name VARCHAR(50) NOT NULL COMMENT 'qwen/deepseek/openai',
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    model VARCHAR(100),
    embedding_url VARCHAR(500),
    embedding_model VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO user (username, password, email) VALUES ('admin', '$2a$10$YourBCryptHashHere', 'admin@example.com');
