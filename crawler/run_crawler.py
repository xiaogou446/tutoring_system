import argparse

from tutor_crawler.fetcher import HttpFetcher
from tutor_crawler.llm_client import RelayLlmClient
from tutor_crawler.llm_parser import LlmTutoringInfoParser
from tutor_crawler.parser import TutoringInfoParser
from tutor_crawler.scheduler import DailyScanScheduler
from tutor_crawler.service import CrawlService
from tutor_crawler.storage import create_storage


def main() -> None:
    parser = argparse.ArgumentParser(description="家教信息采集任务")
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
    parser.add_argument("--list-url", required=True, help="公众号列表页 URL")
    parser.add_argument(
        "--platform-code",
        default="MIAOMIAO_WECHAT",
        help="平台编码，默认 MIAOMIAO_WECHAT",
    )
    parser.add_argument("--include-pattern", default="", help="文章 URL 过滤正则")
    parser.add_argument("--retry-task-id", type=int, default=0, help="重试失败任务 ID")
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
    parser.add_argument(
        "--schedule-daily",
        action="store_true",
        help="按每日固定时间持续调度自动采集",
    )
    parser.add_argument(
        "--daily-run-at",
        default="08:00",
        help="每日调度执行时间，格式 HH:MM",
    )
    parser.add_argument(
        "--poll-seconds",
        type=int,
        default=30,
        help="调度轮询间隔秒数",
    )
    args = parser.parse_args()

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

    if args.retry_task_id > 0:
        ok = service.retry_failed_task(args.retry_task_id, max_attempts=3)
        print({"task_id": args.retry_task_id, "retried": ok})
        return

    if args.schedule_daily:
        scheduler = DailyScanScheduler(
            service=service,
            list_url=args.list_url,
            include_pattern=args.include_pattern or None,
            run_at=args.daily_run_at,
        )
        scheduler.run_forever(poll_seconds=args.poll_seconds)
        return

    result = service.run_daily_scan(
        list_url=args.list_url,
        include_pattern=args.include_pattern or None,
        platform_code=args.platform_code,
    )
    print(result)


if __name__ == "__main__":
    main()
