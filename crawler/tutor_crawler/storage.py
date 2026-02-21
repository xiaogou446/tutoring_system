import sqlite3
from contextlib import closing

from tutor_crawler.html_archive import store_html


class CrawlStorage:
    def __init__(self, db_path: str):
        self.db_path = db_path

    def _conn(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def init_db(self) -> None:
        with closing(self._conn()) as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS article_raw (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_url TEXT NOT NULL UNIQUE,
                    platform_code TEXT NOT NULL DEFAULT 'MIAOMIAO_WECHAT',
                    title TEXT NOT NULL DEFAULT '',
                    content_html TEXT NOT NULL DEFAULT '',
                    content_text TEXT NOT NULL DEFAULT '',
                    published_at TEXT NOT NULL DEFAULT '',
                    crawled_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS crawl_task (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_url TEXT NOT NULL,
                    platform_code TEXT NOT NULL DEFAULT 'MIAOMIAO_WECHAT',
                    source_type TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(source_url, platform_code)
                );

                CREATE TABLE IF NOT EXISTS crawl_task_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    runtime TEXT NOT NULL DEFAULT 'python',
                    stage TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT '',
                    error_type TEXT NOT NULL DEFAULT '',
                    error_summary TEXT NOT NULL DEFAULT '',
                    error_message TEXT NOT NULL DEFAULT '',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS tutoring_info (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_url TEXT NOT NULL UNIQUE,
                    content_block TEXT NOT NULL DEFAULT '',
                    city TEXT NOT NULL DEFAULT '',
                    district TEXT NOT NULL DEFAULT '',
                    grade TEXT NOT NULL DEFAULT '',
                    subject TEXT NOT NULL DEFAULT '',
                    address TEXT NOT NULL DEFAULT '',
                    time_schedule TEXT NOT NULL DEFAULT '',
                    salary_text TEXT NOT NULL DEFAULT '',
                    teacher_requirement TEXT NOT NULL DEFAULT '',
                    published_at TEXT NOT NULL DEFAULT '',
                    city_snippet TEXT NOT NULL DEFAULT '',
                    district_snippet TEXT NOT NULL DEFAULT '',
                    grade_snippet TEXT NOT NULL DEFAULT '',
                    subject_snippet TEXT NOT NULL DEFAULT '',
                    address_snippet TEXT NOT NULL DEFAULT '',
                    time_schedule_snippet TEXT NOT NULL DEFAULT '',
                    salary_snippet TEXT NOT NULL DEFAULT '',
                    teacher_requirement_snippet TEXT NOT NULL DEFAULT '',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS crawl_platform (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    platform_code TEXT NOT NULL UNIQUE,
                    platform_name TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'ENABLED',
                    description TEXT NOT NULL DEFAULT '',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS admin_user (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'ENABLED',
                    last_login_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """
            )

            columns = conn.execute("PRAGMA table_info(article_raw)").fetchall()
            column_names = {row["name"] for row in columns}
            if "platform_code" not in column_names:
                conn.execute(
                    "ALTER TABLE article_raw ADD COLUMN platform_code TEXT NOT NULL DEFAULT 'MIAOMIAO_WECHAT'"
                )

            columns = conn.execute("PRAGMA table_info(crawl_task)").fetchall()
            column_names = {row["name"] for row in columns}
            if "platform_code" not in column_names:
                conn.execute(
                    "ALTER TABLE crawl_task ADD COLUMN platform_code TEXT NOT NULL DEFAULT 'MIAOMIAO_WECHAT'"
                )

            self._migrate_crawl_task_unique_constraint(conn)

            columns = conn.execute("PRAGMA table_info(crawl_task_log)").fetchall()
            column_names = {row["name"] for row in columns}
            if "runtime" not in column_names:
                conn.execute(
                    "ALTER TABLE crawl_task_log ADD COLUMN runtime TEXT NOT NULL DEFAULT 'python'"
                )
            if "error_summary" not in column_names:
                conn.execute(
                    "ALTER TABLE crawl_task_log ADD COLUMN error_summary TEXT NOT NULL DEFAULT ''"
                )

            columns = conn.execute("PRAGMA table_info(tutoring_info)").fetchall()
            column_names = {row["name"] for row in columns}
            if "content_block" not in column_names:
                conn.execute(
                    "ALTER TABLE tutoring_info ADD COLUMN content_block TEXT NOT NULL DEFAULT ''"
                )

            conn.execute(
                "CREATE INDEX IF NOT EXISTS ix_article_raw_platform_code ON article_raw(platform_code)"
            )
            conn.execute(
                "CREATE INDEX IF NOT EXISTS ix_crawl_task_platform_code ON crawl_task(platform_code)"
            )
            conn.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_crawl_task_source_platform ON crawl_task(source_url, platform_code)"
            )
            conn.execute(
                "CREATE INDEX IF NOT EXISTS ix_crawl_platform_status ON crawl_platform(status)"
            )
            conn.execute(
                "CREATE INDEX IF NOT EXISTS ix_admin_user_status ON admin_user(status)"
            )

            conn.execute(
                """
                INSERT INTO crawl_platform(platform_code, platform_name, status, description)
                SELECT 'MIAOMIAO_WECHAT', '淼淼家教公众号', 'ENABLED', '默认平台'
                WHERE NOT EXISTS (
                    SELECT 1 FROM crawl_platform WHERE platform_code='MIAOMIAO_WECHAT'
                )
                """
            )
            conn.commit()

    @staticmethod
    def _migrate_crawl_task_unique_constraint(conn: sqlite3.Connection) -> None:
        indexes = conn.execute("PRAGMA index_list(crawl_task)").fetchall()
        for index in indexes:
            if int(index["unique"]) != 1:
                continue
            index_name = index["name"]
            columns = conn.execute(f"PRAGMA index_info({index_name!r})").fetchall()
            column_names = [column["name"] for column in columns]
            if column_names == ["source_url", "platform_code"]:
                return

        conn.execute("PRAGMA foreign_keys = OFF")
        conn.execute("DROP TABLE IF EXISTS crawl_task_new")
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS crawl_task_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_url TEXT NOT NULL,
                platform_code TEXT NOT NULL DEFAULT 'MIAOMIAO_WECHAT',
                source_type TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'PENDING',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(source_url, platform_code)
            )
            """
        )
        conn.execute(
            """
            INSERT INTO crawl_task_new(id, source_url, platform_code, source_type, status, created_at, updated_at)
            SELECT id, source_url, IFNULL(NULLIF(platform_code, ''), 'MIAOMIAO_WECHAT'), source_type, status, created_at, updated_at
              FROM crawl_task
            """
        )
        conn.execute("DROP TABLE crawl_task")
        conn.execute("ALTER TABLE crawl_task_new RENAME TO crawl_task")
        conn.execute("PRAGMA foreign_keys = ON")

    def create_task(
        self,
        source_url: str,
        source_type: str,
        platform_code: str = "MIAOMIAO_WECHAT",
    ) -> int:
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT INTO crawl_task(source_url, platform_code, source_type, status)
                VALUES (?, ?, ?, 'PENDING')
                """,
                (source_url, platform_code, source_type),
            )
            conn.commit()
            return int(
                conn.execute("SELECT last_insert_rowid() AS id").fetchone()["id"]
            )

    def get_task_by_url(
        self,
        source_url: str,
        platform_code: str = "MIAOMIAO_WECHAT",
    ) -> sqlite3.Row | None:
        with closing(self._conn()) as conn:
            return conn.execute(
                "SELECT * FROM crawl_task WHERE source_url=? AND platform_code=?",
                (source_url, platform_code),
            ).fetchone()

    def get_task(self, task_id: int) -> sqlite3.Row | None:
        with closing(self._conn()) as conn:
            return conn.execute(
                "SELECT * FROM crawl_task WHERE id=?", (task_id,)
            ).fetchone()

    def update_task_status(self, task_id: int, status: str) -> None:
        with closing(self._conn()) as conn:
            conn.execute(
                """
                UPDATE crawl_task
                   SET status=?, updated_at=CURRENT_TIMESTAMP
                 WHERE id=?
                """,
                (status, task_id),
            )
            conn.commit()

    def add_task_log(
        self,
        task_id: int,
        stage: str,
        status: str,
        error_type: str = "",
        error_summary: str = "",
        error_message: str = "",
        runtime: str = "python",
    ) -> None:
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT INTO crawl_task_log(task_id, runtime, stage, status, error_type, error_summary, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    task_id,
                    runtime,
                    stage,
                    status,
                    error_type,
                    error_summary,
                    error_message,
                ),
            )
            conn.commit()

    def list_task_logs(self, task_id: int) -> list[sqlite3.Row]:
        with closing(self._conn()) as conn:
            rows = conn.execute(
                "SELECT * FROM crawl_task_log WHERE task_id=? ORDER BY id ASC",
                (task_id,),
            ).fetchall()
            return list(rows)

    def save_article(self, article: dict) -> None:
        html_path = store_html(article["source_url"], article["content_html"])
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO article_raw(
                    source_url, platform_code, title, content_html, content_text, published_at, crawled_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
            conn.execute(
                """
                INSERT OR REPLACE INTO tutoring_info(
                    source_url, content_block, city, district, grade, subject, address,
                    time_schedule, salary_text, teacher_requirement, published_at,
                    city_snippet, district_snippet, grade_snippet, subject_snippet,
                    address_snippet, time_schedule_snippet, salary_snippet, teacher_requirement_snippet,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
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
            conn.execute(
                "DELETE FROM tutoring_info WHERE source_url=? OR source_url LIKE ?",
                (source_url, f"{source_url}#item-%"),
            )
            conn.commit()

    def get_tutoring_info_by_url(self, source_url: str) -> sqlite3.Row | None:
        with closing(self._conn()) as conn:
            return conn.execute(
                "SELECT * FROM tutoring_info WHERE source_url=?", (source_url,)
            ).fetchone()

    def count_articles(self) -> int:
        with closing(self._conn()) as conn:
            return int(
                conn.execute("SELECT COUNT(*) AS c FROM article_raw").fetchone()["c"]
            )

    def count_tutoring_info(self) -> int:
        with closing(self._conn()) as conn:
            return int(
                conn.execute("SELECT COUNT(*) AS c FROM tutoring_info").fetchone()["c"]
            )

    def override_task_created_date(self, task_id: int, day: str) -> None:
        with closing(self._conn()) as conn:
            conn.execute(
                "UPDATE crawl_task SET created_at=? || ' 00:00:00' WHERE id=?",
                (day, task_id),
            )
            conn.commit()

    def daily_success_rate(self, day: str) -> dict:
        with closing(self._conn()) as conn:
            rows = conn.execute(
                """
                SELECT status, COUNT(*) AS c
                  FROM crawl_task
                 WHERE date(created_at)=date(?)
                 GROUP BY status
                """,
                (day,),
            ).fetchall()

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


def create_storage(
    db_type: str,
    sqlite_path: str = "crawler.db",
    mysql_host: str = "127.0.0.1",
    mysql_port: int = 3306,
    mysql_user: str = "root",
    mysql_password: str = "",
    mysql_database: str = "tutoring_crawler",
):
    if db_type == "sqlite":
        return CrawlStorage(sqlite_path)
    if db_type == "mysql":
        from tutor_crawler.storage_mysql import MySQLCrawlStorage

        return MySQLCrawlStorage(
            host=mysql_host,
            port=mysql_port,
            user=mysql_user,
            password=mysql_password,
            database=mysql_database,
        )
    raise ValueError(f"unsupported db_type: {db_type}")
