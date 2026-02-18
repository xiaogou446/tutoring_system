from urllib.request import Request, urlopen


class HttpFetcher:
    def __init__(self, timeout: int = 10) -> None:
        self.timeout = timeout

    def fetch(self, url: str) -> str:
        html = self._fetch_once(url, self._desktop_headers())
        if self._looks_like_wechat_block(html):
            html = self._fetch_once(url, self._mobile_headers())
        return html

    def _fetch_once(self, url: str, headers: dict[str, str]) -> str:
        request = Request(url, headers=headers)
        with urlopen(request, timeout=self.timeout) as response:  # noqa: S310
            return response.read().decode("utf-8", errors="ignore")

    @staticmethod
    def _desktop_headers() -> dict[str, str]:
        return {
            "User-Agent": (
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/130.0.0.0 Safari/537.36"
            ),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Referer": "https://mp.weixin.qq.com/",
        }

    @staticmethod
    def _mobile_headers() -> dict[str, str]:
        return {
            "User-Agent": (
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) "
                "AppleWebKit/605.1.15 (KHTML, like Gecko) "
                "Version/16.6 Mobile/15E148 Safari/604.1"
            ),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Referer": "https://mp.weixin.qq.com/",
        }

    @staticmethod
    def _looks_like_wechat_block(html: str) -> bool:
        block_signals = ["环境异常", "weui-msg__title", "完成验证后可继续访问"]
        has_block_signal = any(signal in html for signal in block_signals)
        has_real_article = 'id="js_content"' in html
        return has_block_signal and not has_real_article
