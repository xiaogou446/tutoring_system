from tutor_crawler.article import parse_article_html
from tutor_crawler.discovery import discover_article_urls


class CrawlService:
    def __init__(self, storage, list_fetcher, article_fetcher, parser) -> None:
        self.storage = storage
        self.list_fetcher = list_fetcher
        self.article_fetcher = article_fetcher
        self.parser = parser

    def run_daily_scan(self, list_url: str, include_pattern: str | None = None) -> dict:
        list_html = self.list_fetcher.fetch(list_url)
        urls = discover_article_urls(
            list_html, base_url=list_url, include_pattern=include_pattern
        )
        created_tasks = 0
        succeeded = 0
        failed = 0

        for source_url in urls:
            task = self.storage.get_task_by_url(source_url)
            if task:
                continue

            task_id = self.storage.create_task(source_url, "AUTO")
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
        self.storage.update_task_status(task_id, "RUNNING")
        self.storage.add_task_log(task_id, "FETCH", "RUNNING")

        try:
            html = self.article_fetcher.fetch(source_url)
            article = parse_article_html(source_url=source_url, html=html)
            self.storage.add_task_log(task_id, "FETCH", "SUCCESS")

            self.storage.add_task_log(task_id, "PARSE", "RUNNING")
            infos = (
                self.parser.parse_many(article)
                if hasattr(self.parser, "parse_many")
                else [self.parser.parse(article)]
            )
            infos = [info for info in infos if self._is_meaningful_info(info)]

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

            self.storage.save_article(article)

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
