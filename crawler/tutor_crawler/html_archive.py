import hashlib
import os
from pathlib import Path


_CRAWLER_ROOT = Path(__file__).resolve().parents[1]
_DEFAULT_ARCHIVE_DIR = _CRAWLER_ROOT / "data" / "html"
_RELATIVE_PREFIX = Path("data") / "html"


def _resolve_archive_dir() -> Path:
    override = os.environ.get("TUTOR_CRAWLER_HTML_DIR", "").strip()
    if override:
        return Path(override).expanduser().resolve()
    return _DEFAULT_ARCHIVE_DIR


def store_html(source_url: str, html: str) -> str:
    digest = hashlib.sha1(source_url.encode("utf-8")).hexdigest()
    file_name = f"{digest}.html"
    archive_dir = _resolve_archive_dir()
    archive_dir.mkdir(parents=True, exist_ok=True)
    file_path = archive_dir / file_name
    file_path.write_text(html, encoding="utf-8")
    return str(_RELATIVE_PREFIX / file_name)


def read_html(stored_value: str) -> str:
    value = (stored_value or "").strip()
    if not value:
        return ""

    # 兼容老数据：历史记录里可能仍是原始 HTML 文本。
    if "<" in value and ">" in value:
        return value

    path = Path(value)
    candidates: list[Path] = []
    if path.is_absolute():
        candidates.append(path)
    candidates.append(_resolve_archive_dir() / path.name)
    candidates.append(_CRAWLER_ROOT / path)

    for candidate in candidates:
        if candidate.exists() and candidate.is_file():
            return candidate.read_text(encoding="utf-8")
    return ""
