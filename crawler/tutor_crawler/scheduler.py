import time
from datetime import date, datetime
from typing import Callable


class DailyScanScheduler:
    def __init__(
        self,
        service,
        list_url: str,
        include_pattern: str | None = None,
        run_at: str = "08:00",
        now_func: Callable[[], datetime] | None = None,
        sleep_func: Callable[[float], None] | None = None,
    ) -> None:
        self.service = service
        self.list_url = list_url
        self.include_pattern = include_pattern
        self.run_hour, self.run_minute = self._parse_run_at(run_at)
        self.now_func = now_func or datetime.now
        self.sleep_func = sleep_func or time.sleep
        self.last_run_date: date | None = None

    @staticmethod
    def _parse_run_at(run_at: str) -> tuple[int, int]:
        try:
            hour_text, minute_text = run_at.split(":", maxsplit=1)
            hour = int(hour_text)
            minute = int(minute_text)
        except Exception as ex:  # noqa: BLE001
            raise ValueError("run_at 必须为 HH:MM 格式") from ex

        if hour < 0 or hour > 23 or minute < 0 or minute > 59:
            raise ValueError("run_at 时间范围非法")
        return hour, minute

    def _should_run(self, now: datetime) -> bool:
        if self.last_run_date == now.date():
            return False
        if now.hour > self.run_hour:
            return True
        if now.hour == self.run_hour and now.minute >= self.run_minute:
            return True
        return False

    def run_forever(
        self,
        poll_seconds: int = 30,
        stop_after_runs: int | None = None,
        max_ticks: int | None = None,
    ) -> None:
        runs = 0
        ticks = 0
        while True:
            now = self.now_func()
            if self._should_run(now):
                self.service.run_daily_scan(
                    list_url=self.list_url,
                    include_pattern=self.include_pattern,
                )
                self.last_run_date = now.date()
                runs += 1
                if stop_after_runs is not None and runs >= stop_after_runs:
                    return

            ticks += 1
            if max_ticks is not None and ticks >= max_ticks:
                return

            self.sleep_func(poll_seconds)
