import argparse
import json
from contextlib import closing

from tutor_crawler.fetcher import HttpFetcher
from tutor_crawler.llm_client import RelayLlmClient
from tutor_crawler.llm_parser import LlmTutoringInfoParser
from tutor_crawler.parser import TutoringInfoParser
from tutor_crawler.service import CrawlService
from tutor_crawler.storage import create_storage


def _load_urls(url_args: list[str], url_file: str) -> list[str]:
    urls: list[str] = []

    for value in url_args:
        url = value.strip()
        if url:
            urls.append(url)

    if url_file:
        with open(url_file, "r", encoding="utf-8") as file:
            for line in file:
                raw = line.strip()
                if not raw or raw.startswith("#"):
                    continue
                urls.append(raw)

    deduped: list[str] = []
    seen: set[str] = set()
    for url in urls:
        if url in seen:
            continue
        seen.add(url)
        deduped.append(url)

    return deduped


def _get_db_row_count(storage, source_url: str) -> int:
    sql_mysql = (
        "SELECT COUNT(*) AS c FROM tutoring_info "
        "WHERE source_url=%s OR source_url LIKE %s"
    )
    sql_sqlite = (
        "SELECT COUNT(*) AS c FROM tutoring_info "
        "WHERE source_url=? OR source_url LIKE ?"
    )
    params = (source_url, f"{source_url}#item-%")
    sql = sql_mysql if hasattr(storage, "database") else sql_sqlite

    with closing(storage._conn()) as conn:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        row = cursor.fetchone()
        cursor.close()
        return int(row["c"])


def _load_article_from_raw(storage, source_url: str) -> dict | None:
    sql_mysql = (
        "SELECT source_url, title, content_html, content_text, published_at "
        "FROM article_raw WHERE source_url=%s"
    )
    sql_sqlite = (
        "SELECT source_url, title, content_html, content_text, published_at "
        "FROM article_raw WHERE source_url=?"
    )
    sql = sql_mysql if hasattr(storage, "database") else sql_sqlite

    with closing(storage._conn()) as conn:
        cursor = conn.cursor()
        cursor.execute(sql, (source_url,))
        row = cursor.fetchone()
        cursor.close()
        return row


def _process_from_article_raw(service: CrawlService, task_id: int) -> bool:
    task = service.storage.get_task(task_id)
    if not task:
        return False

    source_url = task["source_url"]
    service.storage.update_task_status(task_id, "RUNNING")
    service.storage.add_task_log(task_id, "FETCH", "SUCCESS", "", "from article_raw")

    article = _load_article_from_raw(service.storage, source_url)
    if not article:
        service.storage.add_task_log(
            task_id,
            "PARSE",
            "FAILED",
            "ARTICLE_NOT_FOUND",
            "article_raw not found",
        )
        service.storage.update_task_status(task_id, "FAILED")
        return False

    service.storage.add_task_log(task_id, "PARSE", "RUNNING")
    infos = (
        service.parser.parse_many(article)
        if hasattr(service.parser, "parse_many")
        else [service.parser.parse(article)]
    )
    infos = [info for info in infos if service._is_meaningful_info(info)]
    if not infos:
        service.storage.add_task_log(
            task_id,
            "PARSE",
            "FAILED",
            "EMPTY_PARSED_RESULT",
            "no meaningful parsed fields",
        )
        service.storage.update_task_status(task_id, "FAILED")
        return False

    service.storage.delete_tutoring_info_by_article(source_url)
    for info in infos:
        service.storage.save_tutoring_info(info)

    service.storage.add_task_log(task_id, "PARSE", "SUCCESS")
    service.storage.update_task_status(task_id, "SUCCESS")
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="按URL导入家教文章（支持批量）")
    parser.add_argument(
        "--url", action="append", default=[], help="单个文章URL，可重复"
    )
    parser.add_argument("--url-file", default="", help="批量URL文件，每行一个URL")
    parser.add_argument(
        "--from-article-raw",
        action="store_true",
        help="从article_raw重解析写入tutoring_info，不重新抓取页面",
    )
    parser.add_argument(
        "--source-type",
        default="MANUAL",
        choices=["MANUAL", "AUTO"],
        help="新建任务来源类型",
    )
    parser.add_argument(
        "--db-type", default="sqlite", choices=["sqlite", "mysql"], help="数据库类型"
    )
    parser.add_argument("--db", default="crawler.db", help="SQLite 数据库路径")
    parser.add_argument("--mysql-host", default="127.0.0.1", help="MySQL 主机")
    parser.add_argument("--mysql-port", type=int, default=3306, help="MySQL 端口")
    parser.add_argument("--mysql-user", default="root", help="MySQL 用户")
    parser.add_argument("--mysql-password", default="", help="MySQL 密码")
    parser.add_argument(
        "--mysql-database", default="tutoring_crawler", help="MySQL 数据库"
    )
    parser.add_argument(
        "--parser-mode",
        default="llm",
        choices=["llm", "rule"],
        help="解析模式：llm(默认)/rule",
    )
    parser.add_argument(
        "--llm-config",
        default="crawler/llm_config.json",
        help="项目内 LLM 配置文件路径(JSON)",
    )
    parser.add_argument("--llm-base-url", default="", help="中转 LLM Base URL")
    parser.add_argument("--llm-api-key", default="", help="中转 LLM API Key")
    parser.add_argument("--llm-model", default="", help="中转 LLM 模型名")
    parser.add_argument(
        "--llm-timeout-seconds",
        type=int,
        default=0,
        help="LLM 请求超时秒数（<=0 时使用 llm_config.json 配置）",
    )
    args = parser.parse_args()

    urls = _load_urls(args.url, args.url_file)
    if not urls:
        raise SystemExit("至少提供一个 --url 或 --url-file")

    storage = create_storage(
        db_type=args.db_type,
        sqlite_path=args.db,
        mysql_host=args.mysql_host,
        mysql_port=args.mysql_port,
        mysql_user=args.mysql_user,
        mysql_password=args.mysql_password,
        mysql_database=args.mysql_database,
    )
    storage.init_db()

    rule_parser = TutoringInfoParser()
    selected_parser = rule_parser
    if args.parser_mode == "llm":
        llm_client = RelayLlmClient(
            config_path=args.llm_config,
            base_url=args.llm_base_url or None,
            api_key=args.llm_api_key or None,
            model=args.llm_model or None,
            timeout_seconds=args.llm_timeout_seconds
            if args.llm_timeout_seconds > 0
            else None,
        )
        selected_parser = LlmTutoringInfoParser(
            llm_client=llm_client,
            fallback_parser=rule_parser,
            enable_fallback=True,
        )

    service = CrawlService(
        storage=storage,
        list_fetcher=HttpFetcher(),
        article_fetcher=HttpFetcher(),
        parser=selected_parser,
    )

    results: list[dict] = []
    success_count = 0
    for source_url in urls:
        task = storage.get_task_by_url(source_url)
        task_id = (
            int(task["id"])
            if task
            else storage.create_task(source_url, args.source_type)
        )

        ok = (
            _process_from_article_raw(service, task_id)
            if args.from_article_raw
            else service.process_task(task_id)
        )
        rows = _get_db_row_count(storage, source_url)
        success_count += 1 if ok else 0
        results.append(
            {
                "source_url": source_url,
                "task_id": task_id,
                "success": ok,
                "tutoring_rows": rows,
            }
        )

    output = {
        "total": len(urls),
        "success": success_count,
        "failed": len(urls) - success_count,
        "items": results,
    }
    print(json.dumps(output, ensure_ascii=False))


if __name__ == "__main__":
    main()
