-- 功能：将历史单平台数据回填默认平台编码，供多平台能力升级时平滑迁移。
-- 默认平台编码：MIAOMIAO_WECHAT。

-- 1) 存量表结构升级：补齐平台字段与新增主数据表。
ALTER TABLE article_raw
    ADD COLUMN IF NOT EXISTS platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码';

ALTER TABLE crawl_task
    ADD COLUMN IF NOT EXISTS platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码';

CREATE TABLE IF NOT EXISTS crawl_platform (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '平台编码',
    platform_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '平台名称',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '平台状态',
    description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '平台描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='采集平台主数据表';

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL DEFAULT '' COMMENT '管理员用户名',
    password_hash VARCHAR(256) NOT NULL DEFAULT '' COMMENT '密码哈希',
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态',
    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='后台管理员账号表';

CREATE INDEX IF NOT EXISTS ix_article_raw_platform_code ON article_raw(platform_code);
CREATE INDEX IF NOT EXISTS ix_crawl_task_platform_code ON crawl_task(platform_code);
DROP INDEX IF EXISTS ux_crawl_task_source_url ON crawl_task;
CREATE UNIQUE INDEX IF NOT EXISTS ux_crawl_task_source_platform ON crawl_task(source_url, platform_code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_crawl_platform_code ON crawl_platform(platform_code);
CREATE INDEX IF NOT EXISTS ix_crawl_platform_status ON crawl_platform(status);
CREATE UNIQUE INDEX IF NOT EXISTS ux_admin_user_username ON admin_user(username);
CREATE INDEX IF NOT EXISTS ix_admin_user_status ON admin_user(status);

-- 2) 存量数据回填：将历史空平台编码统一修正为默认值。
UPDATE article_raw
SET platform_code = 'MIAOMIAO_WECHAT'
WHERE platform_code IS NULL OR platform_code = '';

UPDATE crawl_task
SET platform_code = 'MIAOMIAO_WECHAT'
WHERE platform_code IS NULL OR platform_code = '';

-- 3) 默认平台主数据兜底插入（幂等）。
INSERT INTO crawl_platform(platform_code, platform_name, status, description)
SELECT 'MIAOMIAO_WECHAT', '淼淼家教公众号', 'ENABLED', '历史默认平台回填记录'
WHERE NOT EXISTS (
    SELECT 1 FROM crawl_platform WHERE platform_code = 'MIAOMIAO_WECHAT'
);
