from tutor_crawler.article import parse_article_html
from tutor_crawler.discovery import discover_article_urls
from tutor_crawler.platform_router import (
    DEFAULT_PLATFORM_CODE,
    ParserProtocol,
    PlatformParserRouter,
)


class CrawlService:
    def __init__(
        self,
        storage,
        list_fetcher,
        article_fetcher,
        parser,
        parser_router: PlatformParserRouter | None = None,
        default_platform_code: str = DEFAULT_PLATFORM_CODE,
    ) -> None:
        self.storage = storage
        self.list_fetcher = list_fetcher
        self.article_fetcher = article_fetcher
        self.default_platform_code = default_platform_code
        self.parser_router = parser_router or PlatformParserRouter(
            default_platform_code=default_platform_code
        )
        if parser_router is None:
            self.parser_router.register(default_platform_code, parser)

    def run_daily_scan(
        self,
        list_url: str,
        include_pattern: str | None = None,
        platform_code: str = DEFAULT_PLATFORM_CODE,
    ) -> dict:
        list_html = self.list_fetcher.fetch(list_url)
        urls = discover_article_urls(
            list_html, base_url=list_url, include_pattern=include_pattern
        )
        created_tasks = 0
        succeeded = 0
        failed = 0

        for source_url in urls:
            task = self.storage.get_task_by_url(source_url, platform_code)
            if task:
                continue

            task_id = self.storage.create_task(
                source_url,
                "AUTO",
                platform_code=platform_code,
            )
            created_tasks += 1
            if self.process_task(task_id):
                succeeded += 1
            else:
                failed += 1

        return {
            "discovered": len(urls),
            "created_tasks": created_tasks,
            "succeeded": succeeded,
            "failed": failed,
        }

    def process_task(self, task_id: int) -> bool:
        task = self.storage.get_task(task_id)
        if not task:
            return False

        source_url = task["source_url"]
        platform_code = self.default_platform_code
        if hasattr(task, "keys") and "platform_code" in task.keys():
            platform_code = task["platform_code"] or self.default_platform_code
        parser = self.resolve_parser(platform_code)
        if not parser:
            self.storage.add_task_log(
                task_id,
                "PARSE",
                "FAILED",
                "UNSUPPORTED_PLATFORM",
                f"unsupported platform_code: {platform_code}",
            )
            self.storage.update_task_status(task_id, "FAILED")
            return False

        self.storage.update_task_status(task_id, "RUNNING")
        self.storage.add_task_log(task_id, "FETCH", "RUNNING")

        try:
            html = self.article_fetcher.fetch(source_url)
            article = parse_article_html(source_url=source_url, html=html)
            article["platform_code"] = platform_code
            self.storage.add_task_log(task_id, "FETCH", "SUCCESS")

            self.storage.add_task_log(task_id, "PARSE", "RUNNING")
            infos = (
                parser.parse_many(article)
                if hasattr(parser, "parse_many")
                else [parser.parse(article)]
            )
            infos = [info for info in infos if self._is_meaningful_info(info)]

            # 即使解析为空，也先持久化 article_raw，便于回溯失败输入与后续重试。
            self.storage.save_article(article)

            if not infos:
                # 若本次未提取到有效结构化数据，保留历史结果，避免因临时页面异常清空既有数据。
                self.storage.add_task_log(
                    task_id,
                    "PARSE",
                    "FAILED",
                    "EMPTY_PARSED_RESULT",
                    "no meaningful parsed fields",
                )
                self.storage.update_task_status(task_id, "FAILED")
                return False

            if hasattr(self.storage, "delete_tutoring_info_by_article"):
                self.storage.delete_tutoring_info_by_article(source_url)

            for info in infos:
                self.storage.save_tutoring_info(info)
            self.storage.add_task_log(task_id, "PARSE", "SUCCESS")
            self.storage.update_task_status(task_id, "SUCCESS")
            return True
        except Exception as ex:  # noqa: BLE001
            self.storage.add_task_log(
                task_id, "EXECUTE", "FAILED", "RUNTIME_ERROR", str(ex)
            )
            self.storage.update_task_status(task_id, "FAILED")
            return False

    def resolve_parser(self, platform_code: str) -> ParserProtocol | None:
        return self.parser_router.resolve(platform_code)

    @staticmethod
    def _is_meaningful_info(info: dict) -> bool:
        key_fields = [
            "city",
            "district",
            "grade",
            "subject",
            "address",
            "time_schedule",
            "salary_text",
            "teacher_requirement",
        ]
        return any((info.get(field) or "").strip() for field in key_fields)

    def retry_failed_task(self, task_id: int, max_attempts: int = 1) -> bool:
        task = self.storage.get_task(task_id)
        if not task or task["status"] != "FAILED":
            return False

        for attempt in range(1, max_attempts + 1):
            self.storage.add_task_log(task_id, f"RETRY_{attempt}", "RUNNING")
            if self.process_task(task_id):
                self.storage.add_task_log(task_id, f"RETRY_{attempt}", "SUCCESS")
                return True
            self.storage.add_task_log(
                task_id, f"RETRY_{attempt}", "FAILED", "RETRY_FAILED", "retry failed"
            )

        return False
