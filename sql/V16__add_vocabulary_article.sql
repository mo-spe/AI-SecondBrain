-- 每日词表与语境文章只属于创建者；截图不持久化到公共文件库。
CREATE TABLE IF NOT EXISTS vocabulary_article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    words_json MEDIUMTEXT NOT NULL,
    article MEDIUMTEXT NOT NULL,
    meanings_json MEDIUMTEXT NOT NULL,
    missing_words_json TEXT NOT NULL,
    topic VARCHAR(120) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_vocabulary_article_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个人每日词表文章';
