import re
from html import unescape
from json import loads
from urllib.parse import urljoin


def discover_article_urls(
    list_html: str, base_url: str, include_pattern: str | None = None
) -> list[str]:
    candidates: list[str] = []

    # 1) 常规 HTML 链接提取。
    candidates.extend(
        re.findall(r"href=['\"]([^'\"]+)['\"]", list_html, flags=re.IGNORECASE)
    )

    # 2) 兼容公众号 appmsg_list JSON：文章链接常放在 data.html（HTML 转义字符串）里。
    try:
        payload = loads(list_html)
        data_html = payload.get("data", {}).get("html", "")
        if isinstance(data_html, str) and data_html:
            decoded_html = unescape(data_html)
            candidates.extend(
                re.findall(
                    r"href=['\"]([^'\"]+)['\"]", decoded_html, flags=re.IGNORECASE
                )
            )
            # 某些模板是文本拼接，不一定在 href 属性中。
            candidates.extend(
                re.findall(
                    r"https?://mp\.weixin\.qq\.com/s\?[^\s'\"<>]+",
                    decoded_html,
                    flags=re.IGNORECASE,
                )
            )
    except Exception:  # noqa: BLE001
        pass

    # 3) 兜底：直接在原始响应里抓取 s? 链接。
    candidates.extend(
        re.findall(
            r"https?://mp\.weixin\.qq\.com/s\?[^\s'\"<>]+",
            list_html,
            flags=re.IGNORECASE,
        )
    )

    urls: list[str] = []
    seen: set[str] = set()
    pattern = re.compile(include_pattern) if include_pattern else None

    for href in candidates:
        normalized = unescape(href.strip())
        normalized = normalized.replace("\\/", "/")
        normalized = normalized.replace("http://", "https://")
        normalized = normalized.replace("#wechat_redirect", "")

        full_url = urljoin(base_url, normalized)
        if pattern and not pattern.search(full_url):
            continue
        if full_url in seen:
            continue
        seen.add(full_url)
        urls.append(full_url)

    return urls
