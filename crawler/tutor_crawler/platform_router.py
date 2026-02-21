from typing import Protocol


DEFAULT_PLATFORM_CODE = "MIAOMIAO_WECHAT"


class ParserProtocol(Protocol):
    def parse(self, article: dict) -> dict: ...

    def parse_many(self, article: dict) -> list[dict]: ...


class PlatformParserRouter:
    def __init__(self, default_platform_code: str = DEFAULT_PLATFORM_CODE) -> None:
        self.default_platform_code = default_platform_code
        self._parsers: dict[str, ParserProtocol] = {}

    def register(self, platform_code: str, parser: ParserProtocol) -> None:
        code = (platform_code or "").strip()
        if not code:
            raise ValueError("platform_code is required")
        self._parsers[code] = parser

    def resolve(self, platform_code: str | None) -> ParserProtocol | None:
        code = (platform_code or "").strip() or self.default_platform_code
        return self._parsers.get(code)
