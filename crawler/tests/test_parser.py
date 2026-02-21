import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tutor_crawler.parser import TutoringInfoParser


class ParserSplitTest(unittest.TestCase):
    def test_should_split_by_background_color_blocks_with_nested_nodes(self):
        parser = TutoringInfoParser()
        html = """
        <div id="js_content">
          <section style="background-color:#f7f7f7">
            <span style="background-color:#f7f7f7">
              【学员信息】：高一<br>
              【辅导科目】：数学<br>
              【地址】：西湖区<br>
              【时间】：周六<br>
              【薪酬】：100/h
            </span>
          </section>
          <section style="background-color:#f7f7f7">
            <span style="background-color:#f7f7f7">
              【学员信息】：初二<br>
              【辅导科目】：英语<br>
              【地址】：滨江区<br>
              【时间】：周日<br>
              【薪酬】：120/h
            </span>
          </section>
        </div>
        """

        article = {
            "source_url": "https://example.com/bg",
            "published_at": "2026-02-20 12:00:00",
            "content_html": html,
            "content_text": "",
        }

        items = parser.parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["source_url"], "https://example.com/bg")
        self.assertEqual(items[1]["source_url"], "https://example.com/bg#item-2")

    def test_should_fallback_to_text_split_when_no_background_color_blocks(self):
        parser = TutoringInfoParser()
        article = {
            "source_url": "https://example.com/text",
            "published_at": "",
            "content_html": "<div id='js_content'><p>普通文本</p></div>",
            "content_text": (
                "学员信息：高一\n"
                "辅导科目：数学\n"
                "地址：西湖区\n"
                "薪酬：100/h\n"
                "学员信息：初二\n"
                "辅导科目：英语\n"
                "地址：滨江区\n"
                "薪酬：120/h"
            ),
        }

        items = parser.parse_many(article)
        self.assertGreaterEqual(len(items), 2)


if __name__ == "__main__":
    unittest.main()
