from contextlib import closing

import pymysql
from pymysql.cursors import DictCursor

from tutor_crawler.html_archive import store_html


class MySQLCrawlStorage:
    def __init__(self, host: str, port: int, user: str, password: str, database: str):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database

    def _conn(self):
        return pymysql.connect(
            host=self.host,
            port=self.port,
            user=self.user,
            password=self.password,
            database=self.database,
            charset="utf8mb4",
            cursorclass=DictCursor,
            autocommit=False,
        )

    def init_db(self) -> None:
        statements = [
            """
            CREATE TABLE IF NOT EXISTS article_raw (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                source_url VARCHAR(512) NOT NULL UNIQUE COMMENT '文章原始URL',
                platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码',
                title VARCHAR(512) NOT NULL DEFAULT '' COMMENT '文章标题',
                content_html LONGTEXT NOT NULL COMMENT '文章原始HTML内容',
                content_text LONGTEXT NOT NULL COMMENT '文章正文纯文本',
                published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文章发布时间',
                crawled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近抓取时间',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章原始数据表'
            """,
            """
            CREATE TABLE IF NOT EXISTS crawl_task (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                source_url VARCHAR(512) NOT NULL COMMENT '任务对应文章URL',
                platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码',
                source_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '任务来源类型(AUTO/MANUAL)',
                status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态(PENDING/RUNNING/SUCCESS/FAILED)',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                UNIQUE KEY ux_crawl_task_source_platform (source_url, platform_code)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抓取任务表'
            """,
            """
            CREATE TABLE IF NOT EXISTS crawl_task_log (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                task_id BIGINT NOT NULL COMMENT '关联任务ID',
                stage VARCHAR(64) NOT NULL DEFAULT '' COMMENT '执行阶段(FETCH/PARSE/RETRY_x)',
                status VARCHAR(32) NOT NULL DEFAULT '' COMMENT '阶段状态(RUNNING/SUCCESS/FAILED)',
                error_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误类型编码',
                error_message VARCHAR(512) NOT NULL DEFAULT '' COMMENT '错误详情',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                KEY idx_task_id (task_id),
                CONSTRAINT fk_task_id FOREIGN KEY (task_id) REFERENCES crawl_task(id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表'
            """,
            """
            CREATE TABLE IF NOT EXISTS tutoring_info (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                source_url VARCHAR(512) NOT NULL UNIQUE COMMENT '文章原始URL',
                content_block LONGTEXT NOT NULL COMMENT '单条家教信息原始分段内容',
                city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '城市',
                district VARCHAR(64) NOT NULL DEFAULT '' COMMENT '区县/行政区',
                grade VARCHAR(64) NOT NULL DEFAULT '' COMMENT '学员年级',
                subject VARCHAR(64) NOT NULL DEFAULT '' COMMENT '辅导科目',
                address VARCHAR(256) NOT NULL DEFAULT '' COMMENT '授课地址',
                time_schedule VARCHAR(256) NOT NULL DEFAULT '' COMMENT '授课时间安排',
                salary_text VARCHAR(128) NOT NULL DEFAULT '' COMMENT '薪酬文本',
                teacher_requirement VARCHAR(512) NOT NULL DEFAULT '' COMMENT '教员要求',
                published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '文章发布时间',
                city_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '城市字段命中片段',
                district_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '区县字段命中片段',
                grade_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '年级字段命中片段',
                subject_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '科目字段命中片段',
                address_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '地址字段命中片段',
                time_schedule_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '时间字段命中片段',
                salary_snippet VARCHAR(256) NOT NULL DEFAULT '' COMMENT '薪酬字段命中片段',
                teacher_requirement_snippet VARCHAR(512) NOT NULL DEFAULT '' COMMENT '教员要求字段命中片段',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家教结构化信息表'
            """,
            """
            CREATE TABLE IF NOT EXISTS crawl_platform (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                platform_code VARCHAR(64) NOT NULL UNIQUE COMMENT '平台编码',
                platform_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '平台名称',
                status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '平台状态',
                description VARCHAR(512) NOT NULL DEFAULT '' COMMENT '平台描述',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                KEY idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集平台主数据表'
            """,
            """
            CREATE TABLE IF NOT EXISTS admin_user (
                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                username VARCHAR(64) NOT NULL UNIQUE COMMENT '管理员用户名',
                password_hash VARCHAR(256) NOT NULL DEFAULT '' COMMENT '密码哈希',
                status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '账号状态',
                last_login_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后登录时间',
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                KEY idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员账号表'
            """,
        ]
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                for sql in statements:
                    cursor.execute(sql)

                cursor.execute(
                    """
                    SELECT COUNT(*) AS c
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='article_raw' AND COLUMN_NAME='platform_code'
                    """,
                    (self.database,),
                )
                article_platform_column = cursor.fetchone()
                if (
                    int(article_platform_column["c"] if article_platform_column else 0)
                    == 0
                ):
                    cursor.execute(
                        """
                        ALTER TABLE article_raw
                        ADD COLUMN platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码'
                        AFTER source_url
                        """
                    )

                cursor.execute(
                    """
                    SELECT COUNT(*) AS c
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='crawl_task' AND COLUMN_NAME='platform_code'
                    """,
                    (self.database,),
                )
                task_platform_column = cursor.fetchone()
                if int(task_platform_column["c"] if task_platform_column else 0) == 0:
                    cursor.execute(
                        """
                        ALTER TABLE crawl_task
                        ADD COLUMN platform_code VARCHAR(64) NOT NULL DEFAULT 'MIAOMIAO_WECHAT' COMMENT '来源平台编码'
                        AFTER source_url
                        """
                    )

                cursor.execute(
                    """
                    SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='crawl_task' AND NON_UNIQUE=0
                    GROUP BY INDEX_NAME
                    """,
                    (self.database,),
                )
                unique_indexes = cursor.fetchall() or []
                has_composite_unique = False
                for index in unique_indexes:
                    cols = (index.get("cols") or "").lower()
                    index_name = index.get("INDEX_NAME")
                    if cols == "source_url,platform_code":
                        has_composite_unique = True
                        continue
                    if cols == "source_url" and index_name and index_name != "PRIMARY":
                        cursor.execute(
                            f"ALTER TABLE crawl_task DROP INDEX `{index_name}`"
                        )

                if not has_composite_unique:
                    cursor.execute(
                        "CREATE UNIQUE INDEX ux_crawl_task_source_platform ON crawl_task(source_url, platform_code)"
                    )

                cursor.execute(
                    """
                    SELECT COUNT(*) AS c
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='tutoring_info' AND COLUMN_NAME='content_block'
                    """,
                    (self.database,),
                )
                content_block_column = cursor.fetchone()
                if int(content_block_column["c"] if content_block_column else 0) == 0:
                    cursor.execute(
                        """
                        ALTER TABLE tutoring_info
                        ADD COLUMN content_block LONGTEXT NOT NULL COMMENT '单条家教信息原始分段内容'
                        AFTER source_url
                        """
                    )

                cursor.execute(
                    """
                    SELECT COUNT(*) AS c
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='article_raw' AND INDEX_NAME='ix_article_raw_platform_code'
                    """,
                    (self.database,),
                )
                article_platform_index = cursor.fetchone()
                if (
                    int(article_platform_index["c"] if article_platform_index else 0)
                    == 0
                ):
                    cursor.execute(
                        "CREATE INDEX ix_article_raw_platform_code ON article_raw(platform_code)"
                    )

                cursor.execute(
                    """
                    SELECT COUNT(*) AS c
                    FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA=%s AND TABLE_NAME='crawl_task' AND INDEX_NAME='ix_crawl_task_platform_code'
                    """,
                    (self.database,),
                )
                task_platform_index = cursor.fetchone()
                if int(task_platform_index["c"] if task_platform_index else 0) == 0:
                    cursor.execute(
                        "CREATE INDEX ix_crawl_task_platform_code ON crawl_task(platform_code)"
                    )
                cursor.execute(
                    """
                    INSERT INTO crawl_platform(platform_code, platform_name, status, description)
                    SELECT 'MIAOMIAO_WECHAT', '淼淼家教公众号', 'ENABLED', '默认平台'
                    WHERE NOT EXISTS (
                        SELECT 1 FROM crawl_platform WHERE platform_code='MIAOMIAO_WECHAT'
                    )
                    """
                )
            conn.commit()

    def create_task(
        self,
        source_url: str,
        source_type: str,
        platform_code: str = "MIAOMIAO_WECHAT",
    ) -> int:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "INSERT INTO crawl_task(source_url, platform_code, source_type, status) VALUES (%s, %s, %s, 'PENDING')",
                    (source_url, platform_code, source_type),
                )
                task_id = int(cursor.lastrowid)
            conn.commit()
            return task_id

    def get_task_by_url(
        self,
        source_url: str,
        platform_code: str = "MIAOMIAO_WECHAT",
    ):
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM crawl_task WHERE source_url=%s AND platform_code=%s",
                    (source_url, platform_code),
                )
                return cursor.fetchone()

    def get_task(self, task_id: int):
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT * FROM crawl_task WHERE id=%s", (task_id,))
                return cursor.fetchone()

    def update_task_status(self, task_id: int, status: str) -> None:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "UPDATE crawl_task SET status=%s, updated_at=CURRENT_TIMESTAMP WHERE id=%s",
                    (status, task_id),
                )
            conn.commit()

    def add_task_log(
        self,
        task_id: int,
        stage: str,
        status: str,
        error_type: str = "",
        error_message: str = "",
    ) -> None:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO crawl_task_log(task_id, stage, status, error_type, error_message)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    (task_id, stage, status, error_type, error_message),
                )
            conn.commit()

    def list_task_logs(self, task_id: int):
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM crawl_task_log WHERE task_id=%s ORDER BY id ASC",
                    (task_id,),
                )
                return list(cursor.fetchall())

    def save_article(self, article: dict) -> None:
        html_path = store_html(article["source_url"], article["content_html"])
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO article_raw(source_url, platform_code, title, content_html, content_text, published_at, crawled_at, updated_at)
                    VALUES (%s, %s, %s, %s, %s, IFNULL(NULLIF(%s, ''), CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        platform_code=VALUES(platform_code),
                        title=VALUES(title),
                        content_html=VALUES(content_html),
                        content_text=VALUES(content_text),
                        published_at=VALUES(published_at),
                        crawled_at=CURRENT_TIMESTAMP,
                        updated_at=CURRENT_TIMESTAMP
                    """,
                    (
                        article["source_url"],
                        article.get("platform_code", "MIAOMIAO_WECHAT"),
                        article["title"],
                        html_path,
                        article["content_text"],
                        article["published_at"],
                    ),
                )
            conn.commit()

    def save_tutoring_info(self, info: dict) -> None:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO tutoring_info(
                        source_url, content_block, city, district, grade, subject, address,
                        time_schedule, salary_text, teacher_requirement, published_at,
                        city_snippet, district_snippet, grade_snippet, subject_snippet,
                        address_snippet, time_schedule_snippet, salary_snippet, teacher_requirement_snippet,
                        updated_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s, %s,
                        %s, %s, %s, IFNULL(NULLIF(%s, ''), CURRENT_TIMESTAMP),
                        %s, %s, %s, %s, %s, %s, %s, %s,
                        CURRENT_TIMESTAMP
                    )
                    ON DUPLICATE KEY UPDATE
                        content_block=VALUES(content_block),
                        city=VALUES(city),
                        district=VALUES(district),
                        grade=VALUES(grade),
                        subject=VALUES(subject),
                        address=VALUES(address),
                        time_schedule=VALUES(time_schedule),
                        salary_text=VALUES(salary_text),
                        teacher_requirement=VALUES(teacher_requirement),
                        published_at=VALUES(published_at),
                        city_snippet=VALUES(city_snippet),
                        district_snippet=VALUES(district_snippet),
                        grade_snippet=VALUES(grade_snippet),
                        subject_snippet=VALUES(subject_snippet),
                        address_snippet=VALUES(address_snippet),
                        time_schedule_snippet=VALUES(time_schedule_snippet),
                        salary_snippet=VALUES(salary_snippet),
                        teacher_requirement_snippet=VALUES(teacher_requirement_snippet),
                        updated_at=CURRENT_TIMESTAMP
                    """,
                    (
                        info["source_url"],
                        info.get("content_block", ""),
                        info["city"],
                        info["district"],
                        info["grade"],
                        info["subject"],
                        info["address"],
                        info["time_schedule"],
                        info["salary_text"],
                        info["teacher_requirement"],
                        info["published_at"],
                        info["city_snippet"],
                        info["district_snippet"],
                        info["grade_snippet"],
                        info["subject_snippet"],
                        info["address_snippet"],
                        info["time_schedule_snippet"],
                        info["salary_snippet"],
                        info["teacher_requirement_snippet"],
                    ),
                )
            conn.commit()

    def delete_tutoring_info_by_article(self, source_url: str) -> None:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "DELETE FROM tutoring_info WHERE source_url=%s OR source_url LIKE %s",
                    (source_url, f"{source_url}#item-%"),
                )
            conn.commit()

    def get_tutoring_info_by_url(self, source_url: str):
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM tutoring_info WHERE source_url=%s", (source_url,)
                )
                return cursor.fetchone()

    def count_articles(self) -> int:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT COUNT(*) AS c FROM article_raw")
                row = cursor.fetchone()
                if not row:
                    return 0
                return int(row.get("c", 0))

    def count_tutoring_info(self) -> int:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT COUNT(*) AS c FROM tutoring_info")
                row = cursor.fetchone()
                if not row:
                    return 0
                return int(row.get("c", 0))

    def override_task_created_date(self, task_id: int, day: str) -> None:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "UPDATE crawl_task SET created_at=CONCAT(%s, ' 00:00:00') WHERE id=%s",
                    (day, task_id),
                )
            conn.commit()

    def daily_success_rate(self, day: str) -> dict:
        with closing(self._conn()) as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT status, COUNT(*) AS c
                      FROM crawl_task
                     WHERE DATE(created_at)=DATE(%s)
                     GROUP BY status
                    """,
                    (day,),
                )
                rows = cursor.fetchall()

        counts = {row["status"]: int(row["c"]) for row in rows}
        total = sum(counts.values())
        success = counts.get("SUCCESS", 0)
        failed = counts.get("FAILED", 0)
        success_rate = (success / total) if total else 0.0
        return {
            "date": day,
            "total": total,
            "success": success,
            "failed": failed,
            "success_rate": success_rate,
        }
