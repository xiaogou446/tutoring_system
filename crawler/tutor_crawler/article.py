import re
from datetime import datetime
from html import unescape
from html.parser import HTMLParser


ITEM_BREAK_MARKER = "<<<ITEM_BREAK>>>"


def parse_article_html(source_url: str, html: str) -> dict:
    title = _extract_title(html)
    published_at = _extract_published_at(html)

    # Prefer WeChat body text to avoid script noise overwhelming parser rules.
    text = _extract_main_text(html)
    return {
        "source_url": source_url,
        "title": title,
        "content_html": html,
        "content_text": text,
        "published_at": published_at,
    }


def _clean_text(text: str) -> str:
    text = unescape(text)
    text = text.replace("\r", "\n")
    text = re.sub(r"[ \t\f\v]+", " ", text)
    text = re.sub(r"\n+", "\n", text)
    return text.strip()


def _normalize_datetime(raw: str) -> str:
    raw = raw.strip()
    formats = ["%Y-%m-%d %H:%M:%S", "%Y-%m-%d"]
    for fmt in formats:
        try:
            dt = datetime.strptime(raw, fmt)
            return dt.strftime("%Y-%m-%d %H:%M:%S")
        except ValueError:
            continue
    return ""


def _extract_title(html: str) -> str:
    title_patterns = [
        r"var\s+msg_title\s*=\s*['\"](.*?)['\"]",
        r"<meta[^>]+property=['\"]og:title['\"][^>]+content=['\"](.*?)['\"]",
        r"<meta[^>]+name=['\"]twitter:title['\"][^>]+content=['\"](.*?)['\"]",
        r"<title>(.*?)</title>",
    ]
    for pattern in title_patterns:
        match = re.search(pattern, html, flags=re.IGNORECASE | re.DOTALL)
        if not match:
            continue
        title = _clean_text(match.group(1))
        if title:
            return title
    return ""


def _extract_published_at(html: str) -> str:
    publish_match = re.search(
        r"(?:article:published_time|publish_time)['\"]\s+content=['\"]([^'\"]+)['\"]",
        html,
        flags=re.IGNORECASE,
    )
    if publish_match:
        published_at = _normalize_datetime(publish_match.group(1))
        if published_at:
            return published_at

    # WeChat often provides unix timestamp in ct variable.
    ct_match = re.search(r"\bvar\s+ct\s*=\s*['\"]?(\d{10})['\"]?", html)
    if ct_match:
        timestamp = int(ct_match.group(1))
        return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")
    return ""


def _extract_main_text(html: str) -> str:
    wechat_blocks = _extract_top_blocks_by_element_id(html, "js_content")
    if len(wechat_blocks) >= 2:
        joined = f"{ITEM_BREAK_MARKER}\n" + f"\n{ITEM_BREAK_MARKER}\n".join(
            _clean_text(block) for block in wechat_blocks
        )
        return _inject_item_breaks(_clean_text(joined))

    wechat_text = _extract_text_by_element_id(html, "js_content")
    if wechat_text:
        return _inject_item_breaks(_clean_text(wechat_text))

    rich_media_blocks = _extract_top_blocks_by_element_id(html, "img-content")
    if len(rich_media_blocks) >= 2:
        joined = f"{ITEM_BREAK_MARKER}\n" + f"\n{ITEM_BREAK_MARKER}\n".join(
            _clean_text(block) for block in rich_media_blocks
        )
        return _inject_item_breaks(_clean_text(joined))

    rich_media_text = _extract_text_by_element_id(html, "img-content")
    if rich_media_text:
        return _inject_item_breaks(_clean_text(rich_media_text))

    return _inject_item_breaks(_clean_text(re.sub(r"<[^>]+>", " ", html)))


def _inject_item_breaks(text: str) -> str:
    if not text or ITEM_BREAK_MARKER in text:
        return text

    lines = [line.strip() for line in text.split("\n") if line.strip()]
    if not lines:
        return text

    start_strong_re = re.compile(
        r"^(?:【)?(?:学员信息|学员|学员情况|年级性别|性别年级科目|信息|年级科目)(?:】)?\s*[:：]"
    )
    start_weak_re = re.compile(
        r"^(?:【)?(?:地址|地点|家庭地址（要有小区名字）|家庭地址|家教地址|辅导地址)(?:】)?\s*[:：]"
    )
    hint_re = re.compile(r"(?:学员|年级|科目|地址|地点|时间|薪|课酬|时薪|要求|信息)")
    core_re = re.compile(r"(?:学员|年级|科目|时间|薪|课酬|时薪|信息)")

    blocks: list[list[str]] = []
    current: list[str] = []
    for line in lines:
        current_hint_count = sum(1 for item in current if hint_re.search(item))
        current_core_count = sum(1 for item in current if core_re.search(item))
        current_has_address = any(start_weak_re.search(item) for item in current)
        if (
            start_strong_re.search(line)
            and current
            and current_hint_count >= 2
            and current_core_count >= 1
        ):
            blocks.append(current)
            current = [line]
        elif (
            start_weak_re.search(line)
            and current
            and current_hint_count >= 2
            and current_has_address
        ):
            blocks.append(current)
            current = [line]
        else:
            current.append(line)

    if current:
        blocks.append(current)

    if len(blocks) < 2:
        single = _trim_block_for_marker(text)
        hint_re = re.compile(
            r"(?:学员|年级|科目|地址|地点|时间|薪|课酬|时薪|要求|信息)"
        )
        if (
            single
            and sum(1 for line in single.split("\n") if hint_re.search(line)) >= 2
        ):
            return f"{ITEM_BREAK_MARKER}\n{single}"
        return text

    merged_blocks = [_trim_block_for_marker("\n".join(block)) for block in blocks]
    merged_blocks = [block for block in merged_blocks if block]
    if len(merged_blocks) < 2:
        return text

    # 在第一条信息前也写入分隔符，便于下游统一按 marker 切块。
    return f"{ITEM_BREAK_MARKER}\n" + f"\n{ITEM_BREAK_MARKER}\n".join(merged_blocks)


def _trim_block_for_marker(block: str) -> str:
    lines = [line.strip() for line in block.split("\n") if line.strip()]
    if not lines:
        return ""

    start_re = re.compile(
        r"^(?:【)?(?:学员信息|学员|学员情况|年级性别|性别年级科目|信息|年级科目|地址|地点|"
        r"家庭地址（要有小区名字）|家庭地址|家教地址|辅导地址|学生年级|辅导科目|科目)(?:】)?\s*[:：]"
    )
    field_re = re.compile(
        r"^(?:【)?(?:学员信息|学员|学员情况|年级性别|性别年级科目|信息|年级科目|地址|地点|"
        r"家庭地址（要有小区名字）|家庭地址|家教地址|辅导地址|学生年级|学生性别|辅导科目|科目|"
        r"补习时间|时间|时间安排|每周几次|几点到几点|平时一周次数|一次上课时长|"
        r"课时费|薪酬|薪资|薪水|课酬|提供时薪|老师要求|教员要求|教师要求|要求|其他备注)(?:】)?\s*[:：]"
    )
    tail_noise_re = re.compile(
        r"^(?:#|END$|淼淼家教$|微信号[:：]|诚意回收家教单|入群|学生证|学历证明|管理员只看|预览时标签不可点|"
        r"方示了各种不同的教学方法和|展示了各种不同的教学方法和)"
    )

    start_index = 0
    for index, line in enumerate(lines):
        if start_re.search(line):
            start_index = index
            break

    candidate = lines[start_index:]
    if not candidate:
        return ""

    end_index = len(candidate)
    seen_field = 0
    for index, line in enumerate(candidate):
        if field_re.search(line):
            seen_field += 1
        if seen_field >= 2 and tail_noise_re.search(line):
            end_index = index
            break

    return "\n".join(candidate[:end_index]).strip()


def _extract_text_by_element_id(html: str, target_id: str) -> str:
    extractor = _ElementTextExtractor(target_id)
    extractor.feed(html)
    extractor.close()
    return extractor.text()


def _extract_top_blocks_by_element_id(html: str, target_id: str) -> list[str]:
    extractor = _TopBlockTextExtractor(target_id)
    extractor.feed(html)
    extractor.close()
    return extractor.blocks()


class _ElementTextExtractor(HTMLParser):
    def __init__(self, target_id: str) -> None:
        super().__init__()
        self.target_id = target_id
        self.capture_depth = 0
        self.ignore_depth = 0
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_dict = {key: value or "" for key, value in attrs}
        if self.capture_depth == 0 and attrs_dict.get("id") == self.target_id:
            self.capture_depth = 1
            return

        if self.capture_depth == 0:
            return

        self.capture_depth += 1
        if tag in {"script", "style"}:
            self.ignore_depth += 1
        if tag in {"br", "p", "div", "li", "section", "tr", "td", "h1", "h2", "h3"}:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if self.capture_depth == 0:
            return

        if tag in {"script", "style"} and self.ignore_depth > 0:
            self.ignore_depth -= 1
        self.capture_depth -= 1
        if self.capture_depth > 0 and tag in {"p", "div", "li", "section", "tr", "td"}:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        if self.capture_depth == 0 or self.ignore_depth > 0:
            return
        self.parts.append(data)

    def text(self) -> str:
        return "".join(self.parts)


class _TopBlockTextExtractor(HTMLParser):
    def __init__(self, target_id: str) -> None:
        super().__init__()
        self.target_id = target_id
        self.depth = 0
        self.capture_root = False
        self.root_depth = 0
        self.root_tag = ""
        self.current_parts: list[str] | None = None
        self.current_tag = ""
        self.current_depth = 0
        self.result_blocks: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.depth += 1
        attrs_dict = {key: value or "" for key, value in attrs}

        if not self.capture_root and attrs_dict.get("id") == self.target_id:
            self.capture_root = True
            self.root_depth = self.depth
            self.root_tag = tag
            return

        if not self.capture_root:
            return

        if (
            self.current_parts is None
            and self.depth == self.root_depth + 1
            and tag in {"div", "section", "article", "li", "tr"}
        ):
            self.current_parts = []
            self.current_tag = tag
            self.current_depth = self.depth

        if self.current_parts is not None and tag in {
            "br",
            "p",
            "li",
            "tr",
            "div",
            "section",
            "article",
        }:
            self.current_parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if self.current_parts is not None and tag in {
            "p",
            "li",
            "tr",
            "div",
            "section",
            "article",
        }:
            self.current_parts.append("\n")

        if (
            self.current_parts is not None
            and self.depth == self.current_depth
            and tag == self.current_tag
        ):
            block_text = "".join(self.current_parts)
            if _clean_text(block_text):
                self.result_blocks.append(block_text)
            self.current_parts = None
            self.current_tag = ""
            self.current_depth = 0

        if self.capture_root and self.depth == self.root_depth and tag == self.root_tag:
            self.capture_root = False
            self.root_depth = 0
            self.root_tag = ""

        self.depth = max(0, self.depth - 1)

    def handle_data(self, data: str) -> None:
        if self.current_parts is None:
            return
        self.current_parts.append(data)

    def blocks(self) -> list[str]:
        normalized = []
        for block in self.result_blocks:
            cleaned = _clean_text(block)
            if cleaned:
                normalized.append(cleaned)
        return normalized
