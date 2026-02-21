import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tutor_crawler.llm_parser import LlmTutoringInfoParser


class _FakeLlmClient:
    def __init__(self, content: str, configured: bool = True) -> None:
        self.content = content
        self.configured = configured
        self.last_user_prompt = ""

    def is_configured(self) -> bool:
        return self.configured

    def chat_completion(self, system_prompt: str, user_prompt: str) -> str:
        self.last_user_prompt = user_prompt
        return self.content


class _FakeFallbackParser:
    def parse_many(self, article: dict):
        return [
            {
                "source_url": article["source_url"],
                "published_at": article.get("published_at", ""),
                "city": "杭州",
                "district": "",
                "grade": "初一",
                "subject": "数学",
                "address": "西湖区",
                "time_schedule": "周六",
                "salary_text": "100/h",
                "teacher_requirement": "认真负责",
                "city_snippet": "",
                "district_snippet": "",
                "grade_snippet": "",
                "subject_snippet": "",
                "address_snippet": "",
                "time_schedule_snippet": "",
                "salary_snippet": "",
                "teacher_requirement_snippet": "",
            }
        ]

    def parse(self, article: dict):
        return self.parse_many(article)[0]


class LlmParserTest(unittest.TestCase):
    def test_should_parse_llm_items_into_standard_rows(self):
        llm_json = """
        {
          "items": [
            {
              "city": "杭州",
              "district": "西湖区",
              "grade": "高一男生",
              "subject": "数学",
              "address": "文三路",
              "time_schedule": "一周一次",
              "salary_text": "150/2小时",
              "teacher_requirement": "认真负责",
              "city_snippet": "",
              "district_snippet": "",
              "grade_snippet": "",
              "subject_snippet": "",
              "address_snippet": "",
              "time_schedule_snippet": "",
              "salary_snippet": "",
              "teacher_requirement_snippet": ""
            },
            {
              "city": "杭州",
              "district": "滨江区",
              "grade": "初二女生",
              "subject": "英语",
              "address": "江南大道",
              "time_schedule": "一周两次",
              "salary_text": "120/2小时",
              "teacher_requirement": "女老师",
              "city_snippet": "",
              "district_snippet": "",
              "grade_snippet": "",
              "subject_snippet": "",
              "address_snippet": "",
              "time_schedule_snippet": "",
              "salary_snippet": "",
              "teacher_requirement_snippet": ""
            }
          ]
        }
        """
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient(llm_json),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/a",
            "published_at": "2026-02-16 10:00:00",
            "content_text": "信息文本",
        }

        items = parser.parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["source_url"], "https://example.com/a")
        self.assertEqual(items[1]["source_url"], "https://example.com/a#item-2")
        self.assertEqual(items[0]["subject"], "数学")
        self.assertEqual(items[1]["subject"], "英语")

    def test_should_fallback_when_llm_not_configured(self):
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient("{}", configured=False),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {"source_url": "https://example.com/b", "content_text": "text"}
        items = parser.parse_many(article)
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["grade"], "初一")

    def test_should_fill_snippet_with_value_when_snippet_missing(self):
        llm_json = """
        {
          "items": [
            {
              "city": "杭州",
              "district": "西湖区",
              "grade": "高一男生",
              "subject": "数学",
              "address": "文三路",
              "time_schedule": "一周一次",
              "salary_text": "150/2小时",
              "teacher_requirement": "认真负责",
              "city_snippet": "",
              "district_snippet": "",
              "grade_snippet": "",
              "subject_snippet": "",
              "address_snippet": "",
              "time_schedule_snippet": "",
              "salary_snippet": "",
              "teacher_requirement_snippet": ""
            }
          ]
        }
        """
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient(llm_json),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/c",
            "published_at": "2026-02-16 10:00:00",
            "content_text": "信息文本",
        }

        item = parser.parse_many(article)[0]
        self.assertEqual(item["grade_snippet"], "高一男生")
        self.assertEqual(item["subject_snippet"], "数学")
        self.assertEqual(item["salary_snippet"], "150/2小时")

    def test_should_add_item_break_marker_before_llm_call(self):
        llm_json = """
        {
          "items": [
            {
              "city": "杭州",
              "district": "",
              "grade": "高一",
              "subject": "数学",
              "address": "西湖区",
              "time_schedule": "周六",
              "salary_text": "100/h",
              "teacher_requirement": "认真负责",
              "city_snippet": "杭州",
              "district_snippet": "",
              "grade_snippet": "高一",
              "subject_snippet": "数学",
              "address_snippet": "西湖区",
              "time_schedule_snippet": "周六",
              "salary_snippet": "100/h",
              "teacher_requirement_snippet": "认真负责"
            }
          ]
        }
        """
        fake_llm = _FakeLlmClient(llm_json)
        parser = LlmTutoringInfoParser(
            llm_client=fake_llm,
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/d",
            "published_at": "",
            "content_text": "学员信息：高一\n辅导科目：数学\n地址：西湖区\n薪酬：100/h\n学员信息：初二\n辅导科目：英语\n地址：滨江区\n薪酬：120/h",
        }

        parser.parse_many(article)
        self.assertIn("<<<ITEM_BREAK>>>", fake_llm.last_user_prompt)

    def test_should_refine_existing_item_break_when_block_contains_multiple_addresses(
        self,
    ):
        llm_json = '{"items":[]}'
        fake_llm = _FakeLlmClient(llm_json)
        parser = LlmTutoringInfoParser(
            llm_client=fake_llm,
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/g",
            "published_at": "",
            "content_text": """
                【学员】：一年级
                【科目】：全科
                【时间】：周末
                【地址】：A小区
                【要求】：有经验
                <<<ITEM_BREAK>>>
                【地址】：B小区
                【科目】：数学
                【时间】：周六
                【薪酬】：100/h
                【地址】：C小区
                【科目】：英语
                【时间】：周日
                【薪酬】：110/h
            """,
        }

        parser.parse_many(article)
        self.assertGreaterEqual(fake_llm.last_user_prompt.count("<<<ITEM_BREAK>>>"), 2)

    def test_should_fallback_when_llm_items_exceed_block_count(self):
        llm_json = """
        {
          "items": [
            {
              "city": "杭州", "district": "", "grade": "高一", "subject": "数学",
              "address": "西湖区", "time_schedule": "周六", "salary_text": "100/h", "teacher_requirement": "",
              "city_snippet": "杭州", "district_snippet": "", "grade_snippet": "高一", "subject_snippet": "数学",
              "address_snippet": "西湖区", "time_schedule_snippet": "周六", "salary_snippet": "100/h", "teacher_requirement_snippet": ""
            },
            {
              "city": "杭州", "district": "", "grade": "初二", "subject": "英语",
              "address": "滨江区", "time_schedule": "周日", "salary_text": "120/h", "teacher_requirement": "",
              "city_snippet": "杭州", "district_snippet": "", "grade_snippet": "初二", "subject_snippet": "英语",
              "address_snippet": "滨江区", "time_schedule_snippet": "周日", "salary_snippet": "120/h", "teacher_requirement_snippet": ""
            },
            {
              "city": "绍兴", "district": "", "grade": "初三", "subject": "物理",
              "address": "越城区", "time_schedule": "周末", "salary_text": "130/h", "teacher_requirement": "",
              "city_snippet": "绍兴", "district_snippet": "", "grade_snippet": "初三", "subject_snippet": "物理",
              "address_snippet": "越城区", "time_schedule_snippet": "周末", "salary_snippet": "130/h", "teacher_requirement_snippet": ""
            }
          ]
        }
        """
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient(llm_json),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/e",
            "published_at": "",
            "content_text": "第一条\n学员\n<<<ITEM_BREAK>>>\n第二条\n学员",
        }

        items = parser.parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["grade"], "初一")

    def test_should_fallback_when_snippet_not_grounded_to_content_block(self):
        llm_json = """
        {
          "items": [
            {
              "city": "杭州", "district": "", "grade": "高一", "subject": "数学",
              "address": "西湖区", "time_schedule": "周六", "salary_text": "100/h", "teacher_requirement": "",
              "city_snippet": "不存在于分块", "district_snippet": "", "grade_snippet": "高一", "subject_snippet": "数学",
              "address_snippet": "西湖区", "time_schedule_snippet": "周六", "salary_snippet": "100/h", "teacher_requirement_snippet": ""
            }
          ]
        }
        """
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient(llm_json),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/f",
            "published_at": "",
            "content_text": "学员信息：高一\n科目：数学\n地址：西湖区\n薪资：100/h\n<<<ITEM_BREAK>>>\n学员信息：初二\n科目：英语",
        }

        items = parser.parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["grade"], "初一")

    def test_should_fallback_with_refined_blocks_when_llm_alignment_failed(self):
        llm_json = '{"items":[]}'
        parser = LlmTutoringInfoParser(
            llm_client=_FakeLlmClient(llm_json),
            fallback_parser=_FakeFallbackParser(),
        )
        article = {
            "source_url": "https://example.com/h",
            "published_at": "2026-02-20 10:00:00",
            "content_text": (
                "【学员】：一年级\n"
                "【科目】：全科\n"
                "【地址】：A小区\n"
                "【薪酬】：100/h\n"
                "<<<ITEM_BREAK>>>\n"
                "【地址】：B小区\n"
                "【科目】：数学\n"
                "【薪酬】：110/h\n"
                "【地址】：C小区\n"
                "【科目】：英语\n"
                "【薪酬】：120/h"
            ),
        }

        items = parser.parse_many(article)
        self.assertEqual(len(items), 3)
        self.assertEqual(items[2]["source_url"], "https://example.com/h#item-3")


if __name__ == "__main__":
    unittest.main()
