CREATE TABLE IF NOT EXISTS article_raw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL,
    title VARCHAR(255),
    publish_time TIMESTAMP NULL,
    html_content CLOB,
    content_text CLOB,
    fetched_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_article_raw_source_url UNIQUE (source_url)
);

CREATE TABLE IF NOT EXISTS crawl_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL,
    task_source VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    article_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_run_at TIMESTAMP NULL,
    CONSTRAINT fk_crawl_task_article_id FOREIGN KEY (article_id) REFERENCES article_raw(id)
);

CREATE TABLE IF NOT EXISTS crawl_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_crawl_task_log_task_id FOREIGN KEY (task_id) REFERENCES crawl_task(id)
);

CREATE TABLE IF NOT EXISTS tutoring_info (
    id BIGINT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL,
    title VARCHAR(255),
    publish_time TIMESTAMP NULL,
    content_text CLOB,
    district VARCHAR(64),
    grade VARCHAR(64),
    subject VARCHAR(64),
    salary VARCHAR(128),
    teacher_gender VARCHAR(32),
    contact VARCHAR(128),
    parsed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tutoring_info_article_raw_id FOREIGN KEY (id) REFERENCES article_raw(id)
);

CREATE INDEX IF NOT EXISTS idx_article_raw_publish_time ON article_raw(publish_time);
CREATE INDEX IF NOT EXISTS idx_crawl_task_status ON crawl_task(status);
CREATE INDEX IF NOT EXISTS idx_crawl_task_source_url ON crawl_task(source_url);
CREATE INDEX IF NOT EXISTS idx_crawl_task_log_task_id ON crawl_task_log(task_id);
CREATE INDEX IF NOT EXISTS idx_tutoring_info_publish_time ON tutoring_info(publish_time);
CREATE INDEX IF NOT EXISTS idx_tutoring_info_district ON tutoring_info(district);
CREATE INDEX IF NOT EXISTS idx_tutoring_info_subject ON tutoring_info(subject);
