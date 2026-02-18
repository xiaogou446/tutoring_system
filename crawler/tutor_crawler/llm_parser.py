import json
import re

from tutor_crawler.llm_client import RelayLlmClient
from tutor_crawler.parser import TutoringInfoParser


SYSTEM_PROMPT = """
你是一个严格的家教信息抽取器。请将输入的 content_text 解析成结构化 JSON。

要求：
1) 只输出 JSON，不要输出任何解释。
2) 输出格式固定为：{"items":[...]}。
3) 每条 item 对应一个“独立信息块”，不同 item 之间绝不能互相借字段。
4) 输入里若出现 `<<<ITEM_BREAK>>>`，必须以此作为唯一分块边界：
   - 一个分块最多输出一个 item。
   - item 的所有字段都只能来自该分块。
5) 每条 item 必须包含字段：
   city, district, grade, subject, address, time_schedule, salary_text, teacher_requirement,
   city_snippet, district_snippet, grade_snippet, subject_snippet, address_snippet,
   time_schedule_snippet, salary_snippet, teacher_requirement_snippet
6) 字段级提取规则（点对点）：
   - city: 仅提取城市名（如 杭州）
   - district: 仅提取区/县/市（如 西湖区）
   - grade: 学员年级/身份原文（保留原样，不做截断）
   - subject: 辅导科目原文
   - address: 上课地址/地点原文
   - time_schedule: 上课时间安排原文
   - salary_text: 薪酬原文（不要数值化）
   - teacher_requirement: 对教员要求原文
7) snippet 规则（强约束）：
   - 每个 `*_snippet` 必须是该分块中的“原文子串”，不得改写。
   - 若该字段有值但没有更长命中片段，snippet 至少填字段值本身。
   - 若字段为空，snippet 必须为空。
8) 禁止猜测：分块内不存在的字段必须返回空字符串。
""".strip()


class LlmTutoringInfoParser:
    """LLM 主解析器：优先走模型，失败时回退规则解析。"""

    def __init__(
        self,
        llm_client: RelayLlmClient | None = None,
        fallback_parser: TutoringInfoParser | None = None,
        enable_fallback: bool = True,
    ) -> None:
        self.llm_client = llm_client or RelayLlmClient()
        self.fallback_parser = fallback_parser or TutoringInfoParser()
        self.enable_fallback = enable_fallback

    def parse(self, article: dict) -> dict:
        items = self.parse_many(article)
        if items:
            return items[0]
        return self.fallback_parser.parse(article)

    def parse_many(self, article: dict) -> list[dict]:
        if not self.llm_client.is_configured():
            return self.fallback_parser.parse_many(article)

        try:
            prepared_text = self._prepare_content_text(article)
            raw_items = self._call_llm(article, prepared_text)
            parsed = self._normalize_items(article, raw_items, prepared_text)
            if parsed:
                return parsed
        except Exception:
            if not self.enable_fallback:
                raise

        return self.fallback_parser.parse_many(article)

    def _call_llm(self, article: dict, content_text: str) -> list[dict]:
        marker = "<<<ITEM_BREAK>>>"
        if marker in content_text:
            blocks = [
                block.strip() for block in content_text.split(marker) if block.strip()
            ]
            if len(blocks) > 8:
                merged_items: list[dict] = []
                for i in range(0, len(blocks), 8):
                    chunk_text = f"\n{marker}\n".join(blocks[i : i + 8])
                    merged_items.extend(self._call_llm_single(article, chunk_text))
                return merged_items

        return self._call_llm_single(article, content_text)

    def _call_llm_single(self, article: dict, content_text: str) -> list[dict]:
        user_prompt = (
            "请解析以下家教文章 content_text，按要求返回 JSON。\n\n"
            f"source_url: {article.get('source_url', '')}\n"
            f"published_at: {article.get('published_at', '')}\n"
            "content_text:\n"
            f"{content_text}"
        )
        content = self.llm_client.chat_completion(SYSTEM_PROMPT, user_prompt)
        payload = self._safe_json_loads(content)
        items = payload.get("items") if isinstance(payload, dict) else None
        if not isinstance(items, list):
            raise RuntimeError("LLM 返回格式错误：缺少 items 列表")
        return [item for item in items if isinstance(item, dict)]

    def _prepare_content_text(self, article: dict) -> str:
        text = article.get("content_text", "")
        if "<<<ITEM_BREAK>>>" in text:
            return text

        blocks: list[str] = []
        if hasattr(self.fallback_parser, "_split_candidate_blocks"):
            try:
                blocks = self.fallback_parser._split_candidate_blocks(text)
            except Exception:  # noqa: BLE001
                blocks = []

        if len(blocks) < 2:
            blocks = self._split_by_start_labels(text)

        if len(blocks) >= 2:
            return "\n<<<ITEM_BREAK>>>\n".join(blocks)
        return text

    @staticmethod
    def _split_by_start_labels(text: str) -> list[str]:
        lines = [line.strip() for line in text.split("\n") if line.strip()]
        if not lines:
            return []

        start_re = re.compile(
            r"^(?:【)?(?:学员信息|学员|年级性别|性别年级科目|信息|地址|地点)(?:】)?\s*[:：]"
        )
        hint_re = re.compile(
            r"(?:学员|年级|科目|地址|地点|时间|薪|课酬|时薪|要求|信息)"
        )

        blocks: list[list[str]] = []
        current: list[str] = []
        for line in lines:
            hint_count = sum(1 for item in current if hint_re.search(item))
            if start_re.search(line) and current and hint_count >= 2:
                blocks.append(current)
                current = [line]
            else:
                current.append(line)

        if current:
            blocks.append(current)

        return [
            "\n".join(block)
            for block in blocks
            if sum(1 for item in block if hint_re.search(item)) >= 2
        ]

    @staticmethod
    def _safe_json_loads(content: str) -> dict:
        text = content.strip()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{[\s\S]*\}", text)
            if not match:
                raise
            return json.loads(match.group(0))

    @staticmethod
    def _normalize_items(
        article: dict, raw_items: list[dict], prepared_text: str
    ) -> list[dict]:
        normalized: list[dict] = []
        source_url = article.get("source_url", "")
        published_at = article.get("published_at", "")
        marker = "<<<ITEM_BREAK>>>"
        blocks = [
            block.strip() for block in prepared_text.split(marker) if block.strip()
        ]
        if not blocks:
            blocks = [prepared_text]

        for index, item in enumerate(raw_items, start=1):
            item_source_url = source_url if index == 1 else f"{source_url}#item-{index}"
            city = (item.get("city") or "").strip()
            district = (item.get("district") or "").strip()
            grade = (item.get("grade") or "").strip()
            subject = (item.get("subject") or "").strip()
            address = (item.get("address") or "").strip()
            time_schedule = (item.get("time_schedule") or "").strip()
            salary_text = (item.get("salary_text") or "").strip()
            teacher_requirement = (item.get("teacher_requirement") or "").strip()

            row = {
                "source_url": item_source_url,
                "published_at": published_at,
                "content_block": blocks[index - 1] if index - 1 < len(blocks) else "",
                "city": city,
                "district": district,
                "grade": grade,
                "subject": subject,
                "address": address,
                "time_schedule": time_schedule,
                "salary_text": salary_text,
                "teacher_requirement": teacher_requirement,
                "city_snippet": (item.get("city_snippet") or "").strip() or city,
                "district_snippet": (item.get("district_snippet") or "").strip()
                or district,
                "grade_snippet": (item.get("grade_snippet") or "").strip() or grade,
                "subject_snippet": (item.get("subject_snippet") or "").strip()
                or subject,
                "address_snippet": (item.get("address_snippet") or "").strip()
                or address,
                "time_schedule_snippet": (
                    item.get("time_schedule_snippet") or ""
                ).strip()
                or time_schedule,
                "salary_snippet": (item.get("salary_snippet") or "").strip()
                or salary_text,
                "teacher_requirement_snippet": (
                    item.get("teacher_requirement_snippet") or ""
                ).strip()
                or teacher_requirement,
            }

            meaningful = any(
                row[field]
                for field in [
                    "city",
                    "district",
                    "grade",
                    "subject",
                    "address",
                    "time_schedule",
                    "salary_text",
                    "teacher_requirement",
                ]
            )
            if meaningful:
                normalized.append(row)

        return normalized
