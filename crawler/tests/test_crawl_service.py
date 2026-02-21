import sys
import tempfile
import unittest
import os
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tutor_crawler.article import parse_article_html
from tutor_crawler.fetcher import HttpFetcher
from tutor_crawler.parser import TutoringInfoParser
from tutor_crawler.platform_router import PlatformParserRouter
from tutor_crawler.service import CrawlService
from tutor_crawler.storage import CrawlStorage
from import_articles import _load_article_from_raw


class FakeFetcher:
    def __init__(self, pages):
        self.pages = pages

    def fetch(self, url: str) -> str:
        if url not in self.pages:
            raise RuntimeError(f"missing page: {url}")
        content = self.pages[url]
        if isinstance(content, Exception):
            raise content
        return content


class FlakyFetcher:
    def __init__(self, fail_times: int, success_html: str):
        self.fail_times = fail_times
        self.success_html = success_html
        self.calls = 0

    def fetch(self, url: str) -> str:
        self.calls += 1
        if self.calls <= self.fail_times:
            raise RuntimeError("temporary network error")
        return self.success_html


class CrawlServiceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tempdir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.tempdir.name) / "crawler.db"
        self.html_archive_dir = Path(self.tempdir.name) / "html-archive"
        self._old_html_archive_env = os.environ.get("TUTOR_CRAWLER_HTML_DIR")
        os.environ["TUTOR_CRAWLER_HTML_DIR"] = str(self.html_archive_dir)
        self.storage = CrawlStorage(str(self.db_path))
        self.storage.init_db()

    def tearDown(self) -> None:
        if self._old_html_archive_env is None:
            os.environ.pop("TUTOR_CRAWLER_HTML_DIR", None)
        else:
            os.environ["TUTOR_CRAWLER_HTML_DIR"] = self._old_html_archive_env
        self.tempdir.cleanup()

    def _article_platform_code(self, source_url: str) -> str:
        with self.storage._conn() as conn:  # noqa: SLF001 - test helper
            row = conn.execute(
                "SELECT platform_code FROM article_raw WHERE source_url=?",
                (source_url,),
            ).fetchone()
            return row["platform_code"] if row else ""

    def _task_count_by_url(self, source_url: str) -> int:
        with self.storage._conn() as conn:  # noqa: SLF001 - test helper
            row = conn.execute(
                "SELECT COUNT(*) AS c FROM crawl_task WHERE source_url=?",
                (source_url,),
            ).fetchone()
            return int(row["c"] if row else 0)

    def test_daily_scan_should_create_increment_tasks_and_parse(self):
        list_url = "https://example.com/list"
        article1 = "https://example.com/a1"
        article2 = "https://example.com/a2"

        list_html = (
            '<a href="https://example.com/a1">a1</a>'
            '<a href="https://example.com/a2">a2</a>'
        )
        article_html = """
            <html>
                <head>
                    <title>家教信息</title>
                    <meta property="article:published_time" content="2026-02-16 10:00:00"/>
                </head>
                <body>
                    城市：上海 区域：浦东 年级：高二 科目：数学 地址：张江
                    时间：周六下午 薪资：300元/2小时 老师要求：有经验
                </body>
            </html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher(
                {article1: article_html, article2: article_html}
            ),
            parser=TutoringInfoParser(),
        )

        first = service.run_daily_scan(list_url)
        second = service.run_daily_scan(list_url)

        self.assertEqual(first["discovered"], 2)
        self.assertEqual(first["created_tasks"], 2)
        self.assertEqual(first["succeeded"], 2)
        self.assertEqual(second["created_tasks"], 0)
        self.assertEqual(self.storage.count_articles(), 2)
        self.assertEqual(self.storage.count_tutoring_info(), 2)

    def test_daily_scan_should_discover_links_from_wechat_appmsg_list_json(self):
        list_url = "https://mp.weixin.qq.com/mp/homepage?action=appmsg_list&f=json"
        article1 = "https://mp.weixin.qq.com/s?__biz=abc&mid=1&idx=1&sn=111"
        article2 = "https://mp.weixin.qq.com/s?__biz=abc&mid=2&idx=1&sn=222"

        list_json = """
            {
              "data": {
                "html": "&lt;a class=\"list_item\" href=\"http://mp.weixin.qq.com/s?__biz=abc&amp;mid=1&amp;idx=1&amp;sn=111#wechat_redirect\"&gt;a1&lt;/a&gt;\n\n&lt;a class=\"list_item\" href=\"http://mp.weixin.qq.com/s?__biz=abc&amp;mid=2&amp;idx=1&amp;sn=222#wechat_redirect\"&gt;a2&lt;/a&gt;"
              }
            }
        """
        article_html = """
            <html>
                <head><title>家教信息</title></head>
                <body>城市：杭州 科目：数学 薪资：120元/2小时</body>
            </html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_json}),
            article_fetcher=FakeFetcher(
                {article1: article_html, article2: article_html}
            ),
            parser=TutoringInfoParser(),
        )

        result = service.run_daily_scan(
            list_url, include_pattern=r"mp\.weixin\.qq\.com/s\?"
        )

        self.assertEqual(result["discovered"], 2)
        self.assertEqual(result["created_tasks"], 2)
        self.assertEqual(self.storage.count_articles(), 2)

    def test_parser_should_allow_missing_fields(self):
        list_url = "https://example.com/list2"
        article_url = "https://example.com/a3"
        list_html = '<a href="https://example.com/a3">a3</a>'
        article_html = """
            <html><head><title>信息2</title></head>
            <body>城市：北京 科目：英语 薪资：面议</body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )

        service.run_daily_scan(list_url)
        parsed = self.storage.get_tutoring_info_by_url(article_url)
        self.assertEqual(parsed["city"], "北京")
        self.assertEqual(parsed["subject"], "英语")
        self.assertEqual(parsed["salary_text"], "面议")
        self.assertEqual(parsed["district"], "")
        self.assertEqual(parsed["teacher_requirement"], "")

    def test_non_tutoring_article_should_persist_article_but_skip_empty_tutoring_info(
        self,
    ):
        list_url = "https://example.com/list-empty"
        article_url = "https://example.com/a-empty"
        list_html = '<a href="https://example.com/a-empty">a-empty</a>'
        article_html = """
            <html><head><title>普通文章</title></head>
            <body>这是一个普通公众号文章，不包含家教字段，仅用于公告。</body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )

        result = service.run_daily_scan(list_url)
        self.assertEqual(result["failed"], 1)
        self.assertEqual(self.storage.count_articles(), 1)
        self.assertEqual(self.storage.count_tutoring_info(), 0)

    def test_retry_should_append_logs_and_mark_success(self):
        task_id = self.storage.create_task("https://example.com/a4", "AUTO")
        self.storage.update_task_status(task_id, "FAILED")

        success_html = """
            <html><head><title>重试成功</title></head>
            <body>城市：杭州 科目：物理 薪资：200元/2小时</body></html>
        """
        flaky_fetcher = FlakyFetcher(fail_times=1, success_html=success_html)
        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=flaky_fetcher,
            parser=TutoringInfoParser(),
        )

        result = service.retry_failed_task(task_id, max_attempts=2)
        task = self.storage.get_task(task_id)
        logs = self.storage.list_task_logs(task_id)

        self.assertTrue(result)
        self.assertEqual(task["status"], "SUCCESS")
        self.assertGreaterEqual(len(logs), 2)

    def test_daily_success_rate_should_include_auto_and_manual(self):
        day = "2026-02-16"
        auto_task = self.storage.create_task("https://example.com/auto", "AUTO")
        manual_task = self.storage.create_task("https://example.com/manual", "MANUAL")
        self.storage.update_task_status(auto_task, "SUCCESS")
        self.storage.update_task_status(manual_task, "FAILED")

        self.storage.override_task_created_date(auto_task, day)
        self.storage.override_task_created_date(manual_task, day)

        stats = self.storage.daily_success_rate(day)
        self.assertEqual(stats["total"], 2)
        self.assertEqual(stats["success"], 1)
        self.assertEqual(stats["failed"], 1)
        self.assertAlmostEqual(stats["success_rate"], 0.5)

    def test_wechat_like_content_should_extract_and_parse_fields(self):
        article_url = "https://mp.weixin.qq.com/s/mock"
        html = """
            <html>
                <head>
                    <title></title>
                    <script>
                        var msg_title = '杭州家教测试';
                        var ct = '1739152800';
                    </script>
                </head>
                <body>
                    <div id="js_content">
                        <p>【所在地区】杭州西湖区</p>
                        <p>【学员年级】六年级</p>
                        <p>【辅导科目】数学、英语</p>
                        <p>【课酬】180元/2小时</p>
                        <p>【授课时间】周六上午</p>
                        <p>【授课地址】地铁2号线古翠路附近</p>
                        <p>【教员要求】女生，有家教经验</p>
                    </div>
                    <script>console.log('noise');</script>
                </body>
            </html>
        """

        article = parse_article_html(source_url=article_url, html=html)
        parsed = TutoringInfoParser().parse(article)

        self.assertEqual(article["title"], "杭州家教测试")
        self.assertEqual(parsed["city"], "杭州")
        self.assertEqual(parsed["district"], "西湖区")
        self.assertEqual(parsed["grade"], "六年级")
        self.assertEqual(parsed["subject"], "数学、英语")
        self.assertEqual(parsed["salary_text"], "180元/2小时")
        self.assertEqual(parsed["time_schedule"], "周六上午")
        self.assertEqual(parsed["address"], "地铁2号线古翠路附近")

    def test_grade_snippet_should_not_keep_extra_desc(self):
        article = {
            "source_url": "https://example.com/grade",
            "published_at": "",
            "title": "杭州家教",
            "content_text": "【学员信息】：六年级，双胞胎\n【辅导科目】：全科",
        }

        parsed = TutoringInfoParser().parse(article)
        self.assertEqual(parsed["grade"], "六年级，双胞胎")

    def test_single_article_should_capture_multiple_tutoring_items(self):
        list_url = "https://example.com/list3"
        article_url = "https://example.com/a-multi"
        list_html = '<a href="https://example.com/a-multi">multi</a>'
        article_html = """
            <html><head><title>多条家教</title></head><body>
            【学员信息】：六年级，双胞胎
            【辅导科目】：全科
            【地址】：上城区彩霞岭社区
            【薪酬】：90-120/h

            【学员信息】：初三
            【辅导科目】：科学
            【地址】：临平星桥街道
            【薪酬】：90每小时
            </body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )
        service.run_daily_scan(list_url)

        self.assertEqual(self.storage.count_tutoring_info(), 2)
        first = self.storage.get_tutoring_info_by_url(article_url)
        second = self.storage.get_tutoring_info_by_url(article_url + "#item-2")
        self.assertEqual(first["grade"], "六年级，双胞胎")
        self.assertEqual(second["grade"], "初三")
        self.assertEqual(first["salary_text"], "90-120/h")
        self.assertEqual(second["salary_text"], "90每小时")
        self.assertIn("【学员信息】：六年级，双胞胎", first["content_block"])
        self.assertIn("【学员信息】：初三", second["content_block"])

    def test_fetcher_should_retry_with_mobile_headers_when_wechat_blocked(self):
        blocked_html = '<div class="weui-msg__title">环境异常</div>'
        normal_html = '<div id="js_content">城市：杭州</div>'

        class _FakeResponse:
            def __init__(self, body: str):
                self.body = body.encode("utf-8")

            def read(self):
                return self.body

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc, tb):
                return False

        fetcher = HttpFetcher()

        with patch("tutor_crawler.fetcher.urlopen") as mock_urlopen:
            mock_urlopen.side_effect = [
                _FakeResponse(blocked_html),
                _FakeResponse(normal_html),
            ]
            html = fetcher.fetch("https://mp.weixin.qq.com/s/mock")

        self.assertIn('id="js_content"', html)
        self.assertEqual(mock_urlopen.call_count, 2)

    def test_multi_item_should_split_when_each_item_starts_with_address(self):
        list_url = "https://example.com/list4"
        article_url = "https://example.com/a-address-start"
        list_html = '<a href="https://example.com/a-address-start">address-start</a>'
        article_html = """
            <html><head><title>地址起始模板</title></head><body>
            地址：钱塘区.多蓝水岸小区银沙苑
            辅导科目：英语
            情况：初一、男，一对一老师上门。
            时间安排：寒假预约；每次2小时（2.3号-元宵、年三十-初六不上、其他都可）
            教员要求：男女不限，英语专业、有家教经验，有责任心。
            老师薪水：70元/小时

            地址：拱墅区.XX花园
            辅导科目：数学
            情况：高一、女
            时间安排：周末白天
            教员要求：理工科优先
            老师薪水：90元/小时
            </body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )
        service.run_daily_scan(list_url)

        self.assertEqual(self.storage.count_tutoring_info(), 2)
        first = self.storage.get_tutoring_info_by_url(article_url)
        second = self.storage.get_tutoring_info_by_url(article_url + "#item-2")
        self.assertEqual(first["address"], "钱塘区.多蓝水岸小区银沙苑")
        self.assertEqual(first["subject"], "英语")
        self.assertEqual(first["grade"], "初一、男，一对一老师上门")
        self.assertEqual(first["salary_text"], "70元/小时")
        self.assertEqual(second["subject"], "数学")
        self.assertEqual(second["grade"], "高一、女")
        self.assertEqual(second["salary_text"], "90元/小时")

    def test_tail_address_should_not_be_split_into_new_item(self):
        article = {
            "source_url": "https://example.com/tail-address",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "【学员】：高二\n"
                "【科目】：数学 理科\n"
                "【课酬】: 一百每小时\n"
                "【时间】: 学生放月假两周课放一四天假，每次放假上两次课，一次课两小时\n"
                "【地址】: 杭州下沙听澜越府\n"
                "【教员要求】：数学 理科优秀\n"
                "#杭州老师#杭州家教\n"
                "淼淼家教\n"
            ),
        }

        items = TutoringInfoParser().parse_many(article)
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0]["address"], "杭州下沙听澜越府")
        self.assertEqual(items[0]["subject"], "数学 理科")
        self.assertEqual(items[0]["grade"], "高二")
        self.assertEqual(
            items[0]["time_schedule"],
            "学生放月假两周课放一四天假，每次放假上两次课，一次课两小时",
        )
        self.assertEqual(items[0]["salary_text"], "一百每小时")

    def test_split_should_handle_address_followed_by_student_fields(self):
        article = {
            "source_url": "https://example.com/split-address-strong",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "地址：萧山区融创悦融湾\n"
                "要求：浙大，小班课教得好\n"
                "【学员】：三年级\n"
                "【科目】：表演课\n"
                "【课酬】: 500/1.5h\n"
                "地址：萧山区悦融湾\n"
                "要求：有经验，认真负责\n"
                "【学员】：六年级\n"
                "【科目】：数学\n"
                "【课酬】: 170/1.5h\n"
            ),
        }

        items = TutoringInfoParser().parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["grade"], "三年级")
        self.assertEqual(items[0]["subject"], "表演课")
        self.assertEqual(items[1]["grade"], "六年级")
        self.assertEqual(items[1]["subject"], "数学")

    def test_parser_should_extract_fields_from_gender_grade_subject_template(self):
        article = {
            "source_url": "https://example.com/template-gender-grade-subject",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "地址: 杭州西湖区之江二小\n"
                "性别年级科目: 男五年级全科\n"
                "补习时间：寒假10天\n"
                "提供时薪：80/H\n"
                "老师要求：女\n"
            ),
        }

        parsed = TutoringInfoParser().parse(article)
        self.assertEqual(parsed["address"], "杭州西湖区之江二小")
        self.assertEqual(parsed["grade"], "五年级")
        self.assertEqual(parsed["subject"], "全科")
        self.assertEqual(parsed["time_schedule"], "寒假10天")
        self.assertEqual(parsed["salary_text"], "80/H")

    def test_parser_should_treat_location_label_as_address(self):
        article = {
            "source_url": "https://example.com/location-label",
            "published_at": "",
            "title": "杭州家教",
            "content_text": "学员：一年级\n科目：全科\n地点：旭辉珺悦府\n薪酬：60/h",
        }

        parsed = TutoringInfoParser().parse(article)
        self.assertEqual(parsed["address"], "旭辉珺悦府")

    def test_parser_should_not_treat_salary_line_as_time_schedule(self):
        article = {
            "source_url": "https://example.com/time-guard",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "地址: 杭州余杭区闲林山水\n"
                "性别年级科目: 女四年级语文\n"
                "补习时间：\n"
                "提供时薪： 80/H\n"
                "老师要求： 女\n"
            ),
        }

        parsed = TutoringInfoParser().parse(article)
        self.assertEqual(parsed["time_schedule"], "")
        self.assertEqual(parsed["salary_text"], "80/H")

    def test_parse_many_should_prefer_dom_blocks_when_available(self):
        article = {
            "source_url": "https://example.com/dom-blocks",
            "published_at": "",
            "title": "杭州家教",
            "content_html": """
                <html><body>
                    <div id=\"js_content\">
                        <div class=\"card\">
                            <p>地址：西湖区文三路</p>
                            <p>信息：高一男生，数学，一周一次，150/2小时</p>
                            <p>要求：认真负责</p>
                        </div>
                        <div class=\"card\">
                            <p>地址：滨江区江南大道</p>
                            <p>信息：初二女生，英语，一周两次，120/2小时</p>
                            <p>要求：女老师</p>
                        </div>
                    </div>
                </body></html>
            """,
            "content_text": "地址：西湖区文三路 信息：高一男生，数学，一周一次，150/2小时 要求：认真负责 地址：滨江区江南大道 信息：初二女生，英语，一周两次，120/2小时 要求：女老师",
        }

        items = TutoringInfoParser().parse_many(article)
        self.assertEqual(len(items), 2)
        self.assertEqual(items[0]["address"], "西湖区文三路")
        self.assertEqual(items[0]["subject"], "数学")
        self.assertEqual(items[1]["address"], "滨江区江南大道")
        self.assertEqual(items[1]["subject"], "英语")

    def test_article_content_text_should_keep_item_break_marker(self):
        html = """
            <html><body>
                <div id="js_content">
                    <div><p>地址：西湖区文三路</p><p>信息：高一男生，数学，150/2小时</p></div>
                    <div><p>地址：滨江区江南大道</p><p>信息：初二女生，英语，120/2小时</p></div>
                </div>
            </body></html>
        """
        article = parse_article_html("https://example.com/marker", html)
        self.assertIn("<<<ITEM_BREAK>>>", article["content_text"])
        self.assertTrue(article["content_text"].startswith("<<<ITEM_BREAK>>>"))

    def test_article_content_text_should_prefer_parser_html_blocks(self):
        html = """
            <html><body>
                <div id="js_content">
                    <div>
                        <p>家教地址：临平区朗诗·未来街区东园西北36米</p>
                        <p>辅导科目：四年级 数学</p>
                    </div>
                    <section style="background-color: #e8f5ff;padding: 6px 10px;">
                        <p>家教地址：西湖区浙江音乐学院</p>
                        <p>辅导科目：政治学考</p>
                        <p>学员情况：高二</p>
                    </section>
                    <section style="background-color: #e8f5ff;padding: 6px 10px;">
                        <p>地址: 滨江区建业路地铁站</p>
                        <p>性别年级科目: 男初一全科</p>
                        <p>补习时间：周一到周日</p>
                    </section>
                </div>
            </body></html>
        """

        article = parse_article_html("https://example.com/prefer-parser-blocks", html)
        blocks = [
            block.strip()
            for block in article["content_text"].split("<<<ITEM_BREAK>>>")
            if block.strip()
        ]

        self.assertEqual(2, len(blocks))
        self.assertIn("学员情况：高二", blocks[0])
        self.assertNotIn("临平区朗诗·未来街区东园西北36米", blocks[0])

    def test_article_content_text_should_trim_head_and_tail_noise_for_single_block(
        self,
    ):
        html = """
            <html><body>
                <div id="js_content">
                    <p>杭州家教</p>
                    <p>兼职老师招聘</p>
                    <p>【学员信息】：六年级，双胞胎</p>
                    <p>【辅导科目】：全科</p>
                    <p>【地址】：上城区彩霞岭社区</p>
                    <p>【要求】：严厉一点，经验丰富一点</p>
                    <p>【薪酬】：90-120/h左右，具体自己报价</p>
                    <p>#杭州老师#杭州家教</p>
                    <p>END</p>
                    <p>淼淼家教</p>
                    <p>微信号：LMFW66666</p>
                </div>
            </body></html>
        """
        article = parse_article_html("https://example.com/noise", html)
        self.assertTrue(
            article["content_text"].startswith("<<<ITEM_BREAK>>>\n【学员信息】：")
        )
        self.assertNotIn("兼职老师招聘", article["content_text"])
        self.assertNotIn("微信号：LMFW66666", article["content_text"])

    def test_first_item_content_block_should_trim_article_preamble(self):
        article = {
            "source_url": "https://example.com/preamble",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "杭州家教\n"
                "兼职老师招聘\n"
                "欢迎优秀的大学生及专职老师\n"
                "家教信息\n"
                "【学员信息】：六年级，双胞胎\n"
                "【辅导科目】：全科\n"
                "【地址】：上城区彩霞岭社区\n"
                "【薪酬】：90-120/h左右，具体自己报价\n"
            ),
        }

        items = TutoringInfoParser().parse_many(article)
        self.assertGreaterEqual(len(items), 1)
        self.assertTrue(items[0]["content_block"].startswith("【学员信息】："))
        self.assertNotIn("兼职老师招聘", items[0]["content_block"])

    def test_last_item_content_block_should_trim_footer_noise(self):
        article = {
            "source_url": "https://example.com/footer-noise",
            "published_at": "",
            "title": "杭州家教",
            "content_text": (
                "【学员】：高二\n"
                "【科目】：数学 理科\n"
                "【课酬】: 一百每小时\n"
                "【时间】: 学生放月假两周课放一四天假，每次放假上两次课，一次课两小时\n"
                "【地址】: 杭州下沙听澜越府\n"
                "【教员要求】：数学 理科优秀\n"
                "#杭州老师#杭州学生#杭州家教\n"
                "END\n"
                "淼淼家教\n"
                "微信号：LMFW66666\n"
            ),
        }

        items = TutoringInfoParser().parse_many(article)
        self.assertEqual(len(items), 1)
        self.assertNotIn("#杭州老师", items[0]["content_block"])
        self.assertNotIn("微信号", items[0]["content_block"])
        self.assertEqual(items[0]["teacher_requirement"], "数学 理科优秀")

    def test_reprocess_should_cleanup_stale_split_items(self):
        article_url = "https://example.com/reparse"
        html = "<html><head><title>重解析</title></head><body>ok</body></html>"

        class _ParserV1:
            def parse_many(self, article):
                base = {
                    "published_at": "",
                    "city": "",
                    "district": "",
                    "grade": "初三",
                    "subject": "数学",
                    "address": "",
                    "time_schedule": "",
                    "salary_text": "",
                    "teacher_requirement": "",
                    "city_snippet": "",
                    "district_snippet": "",
                    "grade_snippet": "",
                    "subject_snippet": "",
                    "address_snippet": "",
                    "time_schedule_snippet": "",
                    "salary_snippet": "",
                    "teacher_requirement_snippet": "",
                }
                return [
                    {**base, "source_url": article["source_url"]},
                    {**base, "source_url": article["source_url"] + "#item-2"},
                ]

        class _ParserV2:
            def parse_many(self, article):
                return [
                    {
                        "source_url": article["source_url"],
                        "published_at": "",
                        "city": "",
                        "district": "",
                        "grade": "高一",
                        "subject": "英语",
                        "address": "",
                        "time_schedule": "",
                        "salary_text": "",
                        "teacher_requirement": "",
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

        task_id = self.storage.create_task(article_url, "MANUAL")
        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher({article_url: html}),
            parser=_ParserV1(),
        )
        service.process_task(task_id)
        self.assertEqual(self.storage.count_tutoring_info(), 2)

        service_v2 = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher({article_url: html}),
            parser=_ParserV2(),
        )
        service_v2.process_task(task_id)
        self.assertEqual(self.storage.count_tutoring_info(), 1)
        self.assertIsNotNone(self.storage.get_tutoring_info_by_url(article_url))
        self.assertIsNone(
            self.storage.get_tutoring_info_by_url(article_url + "#item-2")
        )

    def test_reprocess_empty_parse_should_keep_existing_structured_rows(self):
        article_url = "https://example.com/keep-old-on-empty"
        task_id = self.storage.create_task(article_url, "MANUAL")

        class _ParserOk:
            def parse_many(self, article):
                return [
                    {
                        "source_url": article["source_url"],
                        "published_at": "",
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

        class _ParserEmpty:
            def parse_many(self, article):
                return [
                    {
                        "source_url": article["source_url"],
                        "published_at": "",
                        "city": "",
                        "district": "",
                        "grade": "",
                        "subject": "",
                        "address": "",
                        "time_schedule": "",
                        "salary_text": "",
                        "teacher_requirement": "",
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

        html_ok = "<html><head><title>ok</title></head><body>城市：杭州</body></html>"
        html_empty = "<html><head><title></title></head><body>no fields</body></html>"

        svc_ok = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher({article_url: html_ok}),
            parser=_ParserOk(),
        )
        self.assertTrue(svc_ok.process_task(task_id))
        old = self.storage.get_tutoring_info_by_url(article_url)
        self.assertEqual(old["grade"], "初一")

        svc_empty = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher({article_url: html_empty}),
            parser=_ParserEmpty(),
        )
        self.assertFalse(svc_empty.process_task(task_id))
        kept = self.storage.get_tutoring_info_by_url(article_url)
        self.assertEqual(kept["grade"], "初一")

    def test_daily_scan_should_write_platform_code_to_task_and_article(self):
        list_url = "https://example.com/list-platform"
        article_url = "https://example.com/platform-a1"
        list_html = f'<a href="{article_url}">a1</a>'
        article_html = """
            <html><head><title>平台测试</title></head>
            <body>城市：杭州 科目：数学 薪资：100元/2小时</body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )

        result = service.run_daily_scan(
            list_url=list_url,
            platform_code="MIAOMIAO_WECHAT",
        )

        self.assertEqual(result["succeeded"], 1)
        task = self.storage.get_task_by_url(article_url)
        self.assertIsNotNone(task)
        self.assertEqual(task["platform_code"], "MIAOMIAO_WECHAT")
        self.assertEqual(
            self._article_platform_code(article_url),
            "MIAOMIAO_WECHAT",
        )

    def test_save_article_should_store_html_relative_path_and_write_file(self):
        article_url = "https://example.com/article-html-path"
        html = "<html><body><div id='js_content'>城市：杭州</div></body></html>"
        article = {
            "source_url": article_url,
            "platform_code": "MIAOMIAO_WECHAT",
            "title": "HTML 存储路径测试",
            "content_html": html,
            "content_text": "城市：杭州",
            "published_at": "",
        }

        self.storage.save_article(article)

        with self.storage._conn() as conn:  # noqa: SLF001 - test helper
            row = conn.execute(
                "SELECT content_html FROM article_raw WHERE source_url=?",
                (article_url,),
            ).fetchone()

        stored_value = row["content_html"]
        self.assertTrue(stored_value.startswith("data/html/"))
        html_file = self.html_archive_dir / Path(stored_value).name
        self.assertTrue(html_file.exists())
        self.assertEqual(html_file.read_text(encoding="utf-8"), html)

    def test_load_article_from_raw_should_restore_html_from_stored_relative_path(self):
        article_url = "https://example.com/article-restore-html"
        html = "<html><body><div id='js_content'>地址：西湖区</div></body></html>"
        article = {
            "source_url": article_url,
            "platform_code": "MIAOMIAO_WECHAT",
            "title": "HTML 回放测试",
            "content_html": html,
            "content_text": "地址：西湖区",
            "published_at": "",
        }

        self.storage.save_article(article)
        loaded = _load_article_from_raw(self.storage, article_url)

        self.assertIsNotNone(loaded)
        self.assertEqual(loaded["content_html"], html)

    def test_daily_scan_should_support_same_url_across_different_platforms(self):
        list_url = "https://example.com/list-platform-idempotent"
        article_url = "https://example.com/platform-same-url"
        list_html = f'<a href="{article_url}">a1</a>'
        article_html = """
            <html><head><title>幂等测试</title></head>
            <body>城市：杭州 科目：数学 薪资：100元/2小时</body></html>
        """

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({list_url: list_html}),
            article_fetcher=FakeFetcher({article_url: article_html}),
            parser=TutoringInfoParser(),
        )

        result_a = service.run_daily_scan(
            list_url=list_url,
            platform_code="MIAOMIAO_WECHAT",
        )
        result_b = service.run_daily_scan(
            list_url=list_url,
            platform_code="ALT_PLATFORM",
        )

        self.assertEqual(result_a["created_tasks"], 1)
        self.assertEqual(result_b["created_tasks"], 1)
        self.assertEqual(self._task_count_by_url(article_url), 2)
        self.assertIsNotNone(
            self.storage.get_task_by_url(article_url, "MIAOMIAO_WECHAT")
        )
        self.assertIsNotNone(self.storage.get_task_by_url(article_url, "ALT_PLATFORM"))

    def test_process_task_should_fail_when_platform_not_supported(self):
        article_url = "https://example.com/unsupported-platform"
        task_id = self.storage.create_task(
            article_url,
            "MANUAL",
            platform_code="UNSUPPORTED_PLATFORM",
        )
        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher(
                {
                    article_url: "<html><body>城市：杭州 科目：数学</body></html>",
                }
            ),
            parser=TutoringInfoParser(),
        )

        self.assertFalse(service.process_task(task_id))
        task = self.storage.get_task(task_id)
        self.assertEqual(task["status"], "FAILED")
        logs = self.storage.list_task_logs(task_id)
        self.assertTrue(
            any(log["error_type"] == "UNSUPPORTED_PLATFORM" for log in logs)
        )

    def test_process_task_should_route_parser_by_platform_code(self):
        article_url = "https://example.com/platform-route"
        task_id = self.storage.create_task(
            article_url,
            "MANUAL",
            platform_code="ALT_PLATFORM",
        )

        class _AltParser:
            def parse(self, article):
                return self.parse_many(article)[0]

            def parse_many(self, article):
                return [
                    {
                        "source_url": article["source_url"],
                        "published_at": article.get("published_at", ""),
                        "content_block": article.get("content_text", ""),
                        "city": "杭州",
                        "district": "",
                        "grade": "初二",
                        "subject": "物理",
                        "address": "西湖区",
                        "time_schedule": "周末",
                        "salary_text": "120/2h",
                        "teacher_requirement": "有经验",
                        "city_snippet": "杭州",
                        "district_snippet": "",
                        "grade_snippet": "初二",
                        "subject_snippet": "物理",
                        "address_snippet": "西湖区",
                        "time_schedule_snippet": "周末",
                        "salary_snippet": "120/2h",
                        "teacher_requirement_snippet": "有经验",
                    }
                ]

        router = PlatformParserRouter()
        router.register("MIAOMIAO_WECHAT", TutoringInfoParser())
        router.register("ALT_PLATFORM", _AltParser())

        service = CrawlService(
            storage=self.storage,
            list_fetcher=FakeFetcher({}),
            article_fetcher=FakeFetcher(
                {
                    article_url: "<html><body>this body should be ignored by alt parser</body></html>",
                }
            ),
            parser=TutoringInfoParser(),
            parser_router=router,
        )

        self.assertTrue(service.process_task(task_id))
        parsed = self.storage.get_tutoring_info_by_url(article_url)
        self.assertIsNotNone(parsed)
        self.assertEqual(parsed["subject"], "物理")


if __name__ == "__main__":
    unittest.main()
