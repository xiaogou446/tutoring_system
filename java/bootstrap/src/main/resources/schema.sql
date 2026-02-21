-- 说明：该脚本作为 MVP 的“迁移脚本”，在本地/测试环境通过 Spring SQL init 执行。
-- 后续如引入 Flyway/Liquibase，可将同等内容迁移到版本化脚本目录。

CREATE TABLE IF NOT EXISTS article_raw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '文章来源URL',
    platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码',
    title VARCHAR(512) NOT NULL DEFAULT '' COMMENT '文章标题',
    content_html CLOB NOT NULL DEFAULT '' COMMENT '文章HTML正文',
    content_text CLOB NOT NULL DEFAULT '' COMMENT '文章纯文本正文',
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文章发布时间',
    crawled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抓取时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='原始文章采集表';

CREATE UNIQUE INDEX IF NOT EXISTS ux_article_raw_source_url ON article_raw(source_url);
CREATE INDEX IF NOT EXISTS ix_article_raw_platform_code ON article_raw(platform_code);
CREATE INDEX IF NOT EXISTS ix_article_raw_published_at ON article_raw(published_at);

CREATE TABLE IF NOT EXISTS tutoring_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '来源URL',
    content_block CLOB NOT NULL DEFAULT '' COMMENT '单条家教信息原始分段内容',

    city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '城市',
    district VARCHAR(64) NOT NULL DEFAULT '' COMMENT '区域',
    grade VARCHAR(64) NOT NULL DEFAULT '' COMMENT '年级',
    subject VARCHAR(64) NOT NULL DEFAULT '' COMMENT '科目',
    address VARCHAR(256) NOT NULL DEFAULT '' COMMENT '授课地址',
    time_schedule VARCHAR(256) NOT NULL DEFAULT '' COMMENT '授课时间',
    salary_text VARCHAR(128) NOT NULL DEFAULT '' COMMENT '薪资文本',
    teacher_requirement VARCHAR(512) NOT NULL DEFAULT '' COMMENT '老师要求',

    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '原文发布时间',

    city_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '城市证据片段',
    district_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '区域证据片段',
    grade_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '年级证据片段',
    subject_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '科目证据片段',
    address_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '地址证据片段',
    time_schedule_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '授课时间证据片段',
    salary_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '薪资证据片段',
    teacher_requirement_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '老师要求证据片段',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='家教信息解析结果表';

CREATE UNIQUE INDEX IF NOT EXISTS ux_tutoring_info_source_url ON tutoring_info(source_url);

CREATE INDEX IF NOT EXISTS ix_tutoring_info_published_at ON tutoring_info(published_at);
CREATE INDEX IF NOT EXISTS ix_tutoring_info_city ON tutoring_info(city);
CREATE INDEX IF NOT EXISTS ix_tutoring_info_district ON tutoring_info(district);
CREATE INDEX IF NOT EXISTS ix_tutoring_info_grade ON tutoring_info(grade);
CREATE INDEX IF NOT EXISTS ix_tutoring_info_subject ON tutoring_info(subject);
CREATE INDEX IF NOT EXISTS ix_tutoring_info_salary_text ON tutoring_info(salary_text);

CREATE TABLE IF NOT EXISTS crawl_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_url VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '采集目标URL',
    platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码',
    source_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '来源类型',
    status VARCHAR(32) NOT NULL DEFAULT '' COMMENT '任务状态',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='采集任务主表';

CREATE UNIQUE INDEX IF NOT EXISTS ux_crawl_task_source_platform ON crawl_task(source_url, platform_code);
CREATE INDEX IF NOT EXISTS ix_crawl_task_platform_code ON crawl_task(platform_code);
CREATE INDEX IF NOT EXISTS ix_crawl_task_source_type ON crawl_task(source_type);
CREATE INDEX IF NOT EXISTS ix_crawl_task_status ON crawl_task(status);
CREATE INDEX IF NOT EXISTS ix_crawl_task_created_at ON crawl_task(created_at);

CREATE TABLE IF NOT EXISTS crawl_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL DEFAULT 0 COMMENT '关联任务ID',
    runtime VARCHAR(16) NOT NULL DEFAULT 'python' COMMENT '运行端(java/python)',
    stage VARCHAR(32) NOT NULL DEFAULT '' COMMENT '任务阶段',
    status VARCHAR(32) NOT NULL DEFAULT '' COMMENT '阶段状态',
    error_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误类型',
    error_summary VARCHAR(256) NOT NULL DEFAULT '' COMMENT '失败摘要',
    error_message VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误信息',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阶段开始时间',
    finished_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阶段结束时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_crawl_task_log_task_id FOREIGN KEY (task_id) REFERENCES crawl_task(id)
) COMMENT='采集任务日志表';

CREATE INDEX IF NOT EXISTS ix_crawl_task_log_task_id ON crawl_task_log(task_id);
CREATE INDEX IF NOT EXISTS ix_crawl_task_log_status ON crawl_task_log(status);
CREATE INDEX IF NOT EXISTS ix_crawl_task_log_created_at ON crawl_task_log(created_at);

CREATE TABLE IF NOT EXISTS crawl_platform (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '平台编码',
    platform_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '平台名称',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '平台状态',
    description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '平台描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='采集平台主数据表';

CREATE UNIQUE INDEX IF NOT EXISTS ux_crawl_platform_code ON crawl_platform(platform_code);
CREATE INDEX IF NOT EXISTS ix_crawl_platform_status ON crawl_platform(status);

INSERT INTO crawl_platform(platform_code, platform_name, status, description)
SELECT 'MIAOMIAO_WECHAT', '淼淼家教公众号', 'ENABLED', '默认平台'
WHERE NOT EXISTS (
    SELECT 1 FROM crawl_platform WHERE platform_code = 'MIAOMIAO_WECHAT'
);

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL DEFAULT '' COMMENT '管理员用户名',
    password_hash VARCHAR(256) NOT NULL DEFAULT '' COMMENT '密码哈希',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态',
    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='后台管理员账号表';

CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_user_username ON admin_user(username);
CREATE INDEX IF NOT EXISTS ix_admin_user_status ON admin_user(status);
