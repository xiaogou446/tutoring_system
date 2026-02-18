import sqlite3
from contextlib import closing


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
                    source_url TEXT NOT NULL UNIQUE,
                    source_type TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS crawl_task_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    stage TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT '',
                    error_type TEXT NOT NULL DEFAULT '',
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
                """
            )

            columns = conn.execute("PRAGMA table_info(tutoring_info)").fetchall()
            column_names = {row["name"] for row in columns}
            if "content_block" not in column_names:
                conn.execute(
                    "ALTER TABLE tutoring_info ADD COLUMN content_block TEXT NOT NULL DEFAULT ''"
                )
            conn.commit()

    def create_task(self, source_url: str, source_type: str) -> int:
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT INTO crawl_task(source_url, source_type, status)
                VALUES (?, ?, 'PENDING')
                """,
                (source_url, source_type),
            )
            conn.commit()
            return int(
                conn.execute("SELECT last_insert_rowid() AS id").fetchone()["id"]
            )

    def get_task_by_url(self, source_url: str) -> sqlite3.Row | None:
        with closing(self._conn()) as conn:
            return conn.execute(
                "SELECT * FROM crawl_task WHERE source_url=?", (source_url,)
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
        error_message: str = "",
    ) -> None:
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT INTO crawl_task_log(task_id, stage, status, error_type, error_message)
                VALUES (?, ?, ?, ?, ?)
                """,
                (task_id, stage, status, error_type, error_message),
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
        with closing(self._conn()) as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO article_raw(
                    source_url, title, content_html, content_text, published_at, crawled_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                (
                    article["source_url"],
                    article["title"],
                    article["content_html"],
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
