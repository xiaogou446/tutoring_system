import sys
import unittest
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tutor_crawler.scheduler import DailyScanScheduler


class _FakeService:
    def __init__(self) -> None:
        self.calls: list[tuple[str, str | None]] = []

    def run_daily_scan(self, list_url: str, include_pattern: str | None = None) -> dict:
        self.calls.append((list_url, include_pattern))
        return {"created_tasks": 1, "succeeded": 1, "failed": 0}


class _FakeClock:
    def __init__(self, values: list[datetime]):
        self.values = values
        self.index = 0

    def now(self) -> datetime:
        if self.index >= len(self.values):
            return self.values[-1]
        value = self.values[self.index]
        self.index += 1
        return value


class DailyScanSchedulerTest(unittest.TestCase):
    def test_should_trigger_once_per_day(self):
        service = _FakeService()
        clock = _FakeClock(
            [
                datetime(2026, 2, 16, 7, 59, 0),
                datetime(2026, 2, 16, 8, 0, 0),
                datetime(2026, 2, 16, 8, 10, 0),
                datetime(2026, 2, 17, 8, 0, 0),
            ]
        )
        scheduler = DailyScanScheduler(
            service=service,
            list_url="https://example.com/list",
            include_pattern="mp.weixin.qq.com",
            run_at="08:00",
            now_func=clock.now,
            sleep_func=lambda _: None,
        )

        scheduler.run_forever(stop_after_runs=2, poll_seconds=0, max_ticks=10)

        self.assertEqual(len(service.calls), 2)
        self.assertEqual(service.calls[0][0], "https://example.com/list")
        self.assertEqual(service.calls[0][1], "mp.weixin.qq.com")

    def test_should_reject_invalid_run_time(self):
        with self.assertRaises(ValueError):
            DailyScanScheduler(
                service=_FakeService(),
                list_url="https://example.com/list",
                run_at="8点",
            )


if __name__ == "__main__":
    unittest.main()
