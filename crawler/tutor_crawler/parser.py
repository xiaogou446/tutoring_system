import re
from html.parser import HTMLParser


ITEM_BREAK_MARKER = "<<<ITEM_BREAK>>>"


class TutoringInfoParser:
    def __init__(self) -> None:
        self.value_stop_lookahead = (
            r"(?=\s*(?:[【\[]?(?:所在城市|所在地区|城市|所在地|区域|区县|行政区|所在区|"
            r"学员年级|学生年级|就读年级|学员情况|情况|性别年级科目|信息|辅导科目|授课科目|求教科目|"
            r"授课地址|上课地址|授课地点|上课地点|地点|授课时间|时间安排|上课时间|可授课时间|补习时间|"
            r"薪资|薪酬|薪水|老师薪水|老师薪资|课酬|课时费|提供时薪|报酬|待遇|课时薪酬|老师要求|教员要求|教师要求|教员条件)"
            r"[】\]]?\s*[:：\-]?)|。|\n|$)"
        )
        self.patterns = {
            "city": [
                r"(?:^|\s)[【\[]?(?:城市|所在城市|所在地区|地区|所在地)[】\]]?\s*[:：\-]?\s*([\u4e00-\u9fa5]{2,12})"
            ],
            "district": [
                r"(?:^|\s)[【\[]?(?:区域|区县|行政区|所在区)[】\]]?\s*[:：\-]?\s*([\u4e00-\u9fa5]{1,12}(?:区|县|市))"
            ],
            "grade": [
                rf"(?:^|\s)[【\[]?(?:学员年级|学生年级|就读年级|学员信息|学员|学员情况|情况|年级性别)[】\]]?\s*[:：\-]?\s*([^\n。；;]+?){self.value_stop_lookahead}"
            ],
            "subject": [
                rf"(?:^|\s)[【\[]?(?:辅导科目|授课科目|求教科目|辅导内容|科目|学科)[】\]]?\s*[:：\-]?\s*([^\n。；;]+?){self.value_stop_lookahead}"
            ],
            "address": [
                rf"(?:^|\s)(?:【(?:地址|授课地址|上课地址|授课地点|上课地点|地点)】\s*|[【\[]?(?:地址|授课地址|上课地址|授课地点|上课地点|地点)[】\]]?\s*[:：\-]\s*)([^\n。；;]+?){self.value_stop_lookahead}"
            ],
            "time_schedule": [
                rf"(?:^|\s)[【\[]?(?:时间安排|授课时间|上课时间|可授课时间|补习时间|时间)[】\]]?\s*[:：\-]?\s*([^\n。]+?){self.value_stop_lookahead}"
            ],
            "salary_text": [
                rf"(?:^|\s)[【\[]?(?:薪资|薪酬|薪水|老师薪水|老师薪资|课酬|课时费|提供时薪|报酬|待遇|课时薪酬)[】\]]?\s*[:：\-]?\s*([^\n。；;]+?){self.value_stop_lookahead}",
                r"(面议|自报价|\d+\s*(?:元|￥)\s*/\s*\d+\s*(?:小时|h|H))",
            ],
            "teacher_requirement": [
                rf"(?:^|\s)[【\[]?(?:老师要求|教员要求|教师要求|教员条件|要求)[】\]]?\s*[:：\-]?\s*([^\n。；;]+?){self.value_stop_lookahead}"
            ],
        }

        self.snippet_field_map = {
            "salary_text": "salary_snippet",
        }

    def parse(self, article: dict) -> dict:
        items = self.parse_many(article)
        if items:
            return items[0]

        text = article.get("content_text", "")
        return self._parse_single(article, text, article.get("source_url", ""))

    def parse_many(self, article: dict) -> list[dict]:
        text = article.get("content_text", "")
        html = article.get("content_html", "")
        source_url = article.get("source_url", "")
        html_blocks = self._split_candidate_blocks_from_html(html)
        text_blocks = self._split_candidate_blocks(text)
        should_trim_block = True

        if len(html_blocks) >= 2:
            blocks = html_blocks
            # HTML 分块已由 DOM 结构确定，content_block 保留原块文本，不再做内容裁剪。
            should_trim_block = False
        elif text_blocks:
            blocks = text_blocks
        elif html_blocks:
            blocks = html_blocks
            should_trim_block = False
        else:
            blocks = [text]

        if should_trim_block:
            blocks = [self._trim_block_preamble(block) for block in blocks]
        blocks = [block for block in blocks if block.strip()]

        parsed_items: list[dict] = []
        for index, block in enumerate(blocks, start=1):
            item_source_url = source_url if index == 1 else f"{source_url}#item-{index}"
            parsed = self._parse_single(article, block, item_source_url)
            if self._is_meaningful(parsed):
                parsed_items.append(parsed)

        if parsed_items:
            return parsed_items

        # Keep backward compatibility: if split misses, still parse full text once.
        return [self._parse_single(article, text, source_url)]

    def _parse_single(self, article: dict, text: str, source_url: str) -> dict:
        result = {
            "source_url": source_url,
            "published_at": article.get("published_at", ""),
            "content_block": text.strip(),
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

        for field, rules in self.patterns.items():
            value, snippet = self._extract_first(text, rules)
            result[field] = value
            snippet_field = self.snippet_field_map.get(field, f"{field}_snippet")
            result[snippet_field] = snippet

        self._normalize_location_fields(result)
        self._fill_from_compound_lines(text, result)
        self._infer_city_from_article(article, result)
        self._normalize_text_fields(result)

        return result

    @staticmethod
    def _split_candidate_blocks(text: str) -> list[str]:
        if ITEM_BREAK_MARKER in text:
            marker_blocks = [block.strip() for block in text.split(ITEM_BREAK_MARKER)]
            field_hint = re.compile(
                r"(?:学员信息|学员|学员情况|情况|年级性别|性别年级科目|信息|辅导科目|辅导内容|科目|地址|地点|"
                r"时间|时间安排|补习时间|薪酬|薪资|薪水|老师薪资|老师薪水|课酬|提供时薪|老师要求|要求|教员要求)"
            )
            filtered = [
                block for block in marker_blocks if block and field_hint.search(block)
            ]
            if len(filtered) >= 2:
                return filtered

        lines = [line.strip() for line in text.split("\n") if line.strip()]
        if not lines:
            return []

        # 强起始字段通常明确表示一条新家教信息。
        block_start_strong = re.compile(
            r"^(?:【)?(?:学员信息|学员|年级性别|性别年级科目|信息)(?:】)?\s*[:：]"
        )
        # 弱起始字段用于兼容“地址开头”的模板，但要避免切断同一条信息。
        block_start_weak = re.compile(
            r"^(?:【)?(?:地址|授课地址|上课地址|地点)(?:】)?\s*[:：]"
        )
        field_hint = re.compile(
            r"(?:学员信息|学员|学员情况|情况|年级性别|性别年级科目|信息|辅导科目|辅导内容|科目|地址|地点|"
            r"时间|时间安排|补习时间|薪酬|薪资|薪水|老师薪资|老师薪水|课酬|提供时薪|老师要求|要求|教员要求)"
        )
        strong_field_hint = re.compile(
            r"(?:学员信息|学员|学员情况|情况|年级性别|性别年级科目|信息|辅导科目|辅导内容|科目|"
            r"时间|时间安排|补习时间|薪酬|薪资|薪水|老师薪资|老师薪水|课酬|提供时薪|老师要求|要求|教员要求)"
        )
        core_field_hint = re.compile(
            r"(?:学员信息|学员|年级性别|性别年级科目|信息|辅导科目|辅导内容|科目|"
            r"时间|时间安排|补习时间|薪酬|薪资|薪水|老师薪资|老师薪水|课酬|提供时薪)"
        )

        def weak_start_is_new_item(start_index: int) -> bool:
            # 只看当前“地址”起始后的短窗口，判断其是否构成一条完整家教信息。
            end_index = min(len(lines), start_index + 6)
            segment: list[str] = []
            for index in range(start_index, end_index):
                segment.append(lines[index])

            hint_count = sum(1 for item in segment if field_hint.search(item))
            strong_hint_count = sum(
                1 for item in segment if strong_field_hint.search(item)
            )
            return hint_count >= 3 and strong_hint_count >= 2

        blocks: list[list[str]] = []
        current: list[str] = []

        for index, line in enumerate(lines):
            current_hint_count = sum(1 for item in current if field_hint.search(item))
            current_core_count = sum(
                1 for item in current if core_field_hint.search(item)
            )
            current_has_address = any(block_start_weak.search(item) for item in current)
            if (
                block_start_strong.search(line)
                and current
                and current_hint_count >= 2
                and current_core_count >= 1
            ):
                blocks.append(current)
                current = [line]
            elif (
                block_start_weak.search(line)
                and current
                and current_hint_count >= 2
                and current_has_address
                and weak_start_is_new_item(index)
            ):
                blocks.append(current)
                current = [line]
            else:
                current.append(line)

        if current:
            blocks.append(current)

        normalized_blocks = ["\n".join(block) for block in blocks]
        return [block for block in normalized_blocks if field_hint.search(block)]

    @staticmethod
    def _split_candidate_blocks_from_html(content_html: str) -> list[str]:
        if not content_html:
            return []

        exact_bg_blocks = TutoringInfoParser._split_blocks_from_background_style(
            content_html, strict_signature=True
        )
        if len(exact_bg_blocks) >= 2:
            return exact_bg_blocks

        bg_blocks = TutoringInfoParser._split_blocks_from_background_style(content_html)
        if len(bg_blocks) >= 2:
            return bg_blocks

        field_hint = re.compile(
            r"(?:学员|学员信息|年级|科目|地址|地点|时间|补习时间|薪酬|薪资|薪水|课酬|时薪|要求|信息)"
        )

        class _DomBlockParser(HTMLParser):
            def __init__(self) -> None:
                super().__init__()
                self.depth = 0
                self.in_root = False
                self.root_depth = 0
                self.root_tag = ""
                self.blocks: list[list[str]] = []
                self.current_block: list[str] | None = None
                self.current_block_depth = 0
                self.current_block_tag = ""
                self.container_tags = {"div", "section", "article", "li", "tr"}
                self.line_break_tags = {
                    "p",
                    "li",
                    "tr",
                    "br",
                    "div",
                    "section",
                    "article",
                }
                self.void_tags = {"br", "img", "meta", "link", "input", "hr"}

            def handle_starttag(self, tag: str, attrs) -> None:
                if tag not in self.void_tags:
                    self.depth += 1
                attr_map = {key: value for key, value in attrs}
                node_id = (attr_map.get("id") or "").strip().lower()
                node_class = (attr_map.get("class") or "").strip().lower()

                if not self.in_root and (
                    node_id in {"js_content", "img-content"}
                    or "js_content" in node_class
                    or "img-content" in node_class
                ):
                    self.in_root = True
                    self.root_depth = self.depth
                    self.root_tag = tag

                if (
                    self.in_root
                    and self.current_block is None
                    and self.depth == self.root_depth + 1
                    and tag in self.container_tags
                ):
                    self.current_block = []
                    self.current_block_depth = self.depth
                    self.current_block_tag = tag

                if self.current_block is not None and tag == "br":
                    self.current_block.append("\n")

            def handle_startendtag(self, tag: str, attrs) -> None:
                self.handle_starttag(tag, attrs)

            def handle_endtag(self, tag: str) -> None:
                if self.current_block is not None and tag in self.line_break_tags:
                    self.current_block.append("\n")

                if (
                    self.current_block is not None
                    and self.depth == self.current_block_depth
                    and tag == self.current_block_tag
                ):
                    self.blocks.append(self.current_block)
                    self.current_block = None
                    self.current_block_depth = 0
                    self.current_block_tag = ""

                if (
                    self.in_root
                    and self.depth == self.root_depth
                    and tag == self.root_tag
                ):
                    self.in_root = False
                    self.root_depth = 0
                    self.root_tag = ""

                self.depth = max(0, self.depth - 1)

            def handle_data(self, data: str) -> None:
                if self.current_block is None:
                    return
                text = data.strip()
                if not text:
                    return
                self.current_block.append(text)

        parser = _DomBlockParser()
        parser.feed(content_html)

        normalized: list[str] = []
        for block_tokens in parser.blocks:
            raw = "".join(block_tokens)
            lines = [line.strip() for line in raw.split("\n") if line.strip()]
            if not lines:
                continue
            merged = "\n".join(lines)
            if field_hint.search(merged):
                normalized.append(merged)

        # 去重：避免同一块重复落入（部分嵌套 DOM 会出现重复文本）。
        seen: set[str] = set()
        result: list[str] = []
        for block in normalized:
            if block in seen:
                continue
            seen.add(block)
            result.append(block)
        return result

    @staticmethod
    def _split_blocks_from_background_style(
        content_html: str, strict_signature: bool = False
    ) -> list[str]:
        signature_re = re.compile(
            r"background-color\s*:\s*#e8f5ff\s*;\s*padding", flags=re.IGNORECASE
        )

        class _BackgroundColorBlockParser(HTMLParser):
            def __init__(self) -> None:
                super().__init__()
                self.depth = 0
                self.in_root = False
                self.root_depth = 0
                self.root_tag = ""
                self.nodes: list[dict] = []
                self.stack: list[dict] = []
                self.break_tags = {
                    "br",
                    "p",
                    "li",
                    "tr",
                    "div",
                    "section",
                    "article",
                }
                self.void_tags = {"br", "img", "meta", "link", "input", "hr"}

            def _append_break_to_active_nodes(self) -> None:
                if not self.in_root:
                    return
                for node in self.stack:
                    if node["is_bg"]:
                        node["parts"].append("\n")

            def handle_starttag(self, tag: str, attrs) -> None:
                if tag not in self.void_tags:
                    self.depth += 1
                attr_map = {key: value for key, value in attrs}
                node_id = (attr_map.get("id") or "").strip().lower()
                node_class = (attr_map.get("class") or "").strip().lower()

                if not self.in_root and (
                    node_id in {"js_content", "img-content"}
                    or "js_content" in node_class
                    or "img-content" in node_class
                ):
                    self.in_root = True
                    self.root_depth = self.depth
                    self.root_tag = tag

                style = (attr_map.get("style") or "").lower()
                if strict_signature:
                    is_bg = self.in_root and bool(signature_re.search(style))
                else:
                    is_bg = self.in_root and ("background-color" in style)
                if tag not in self.void_tags:
                    self.stack.append(
                        {"tag": tag, "depth": self.depth, "is_bg": is_bg, "parts": []}
                    )

                if tag in self.break_tags:
                    self._append_break_to_active_nodes()

            def handle_startendtag(self, tag: str, attrs) -> None:
                self.handle_starttag(tag, attrs)

            def handle_endtag(self, tag: str) -> None:
                if tag in self.break_tags:
                    self._append_break_to_active_nodes()

                if self.stack:
                    node = self.stack.pop()
                    if node["is_bg"]:
                        self.nodes.append(node)

                if (
                    self.in_root
                    and self.depth == self.root_depth
                    and tag == self.root_tag
                ):
                    self.in_root = False
                    self.root_depth = 0
                    self.root_tag = ""

                self.depth = max(0, self.depth - 1)

            def handle_data(self, data: str) -> None:
                if not self.in_root:
                    return
                text = data.strip()
                if not text:
                    return
                for node in self.stack:
                    if node["is_bg"]:
                        node["parts"].append(text)

        parser = _BackgroundColorBlockParser()
        parser.feed(content_html)

        normalized_nodes: list[tuple[str, str]] = []
        for node in parser.nodes:
            raw = "".join(node["parts"])
            lines = [line.strip() for line in raw.split("\n") if line.strip()]
            if not lines:
                continue
            merged = "\n".join(lines)
            compact = re.sub(r"\s+", "", merged)
            normalized_nodes.append((merged, compact))

        # 命中目标样式后，仅做文本去重，不叠加额外筛选规则。
        result: list[str] = []
        seen_compact: set[str] = set()
        for text, compact in normalized_nodes:
            if compact in seen_compact:
                continue
            result.append(text)
            seen_compact.add(compact)

        return result

    @staticmethod
    def _trim_block_preamble(block: str) -> str:
        lines = [line.strip() for line in block.split("\n") if line.strip()]
        if not lines:
            return ""

        start_re = re.compile(
            r"^(?:【)?(?:学员信息|学员|年级性别|性别年级科目|信息|地址|地点|家庭地址（要有小区名字）|学生年级|辅导科目|科目)(?:】)?\s*[:：]"
        )

        start_index = 0
        for index, line in enumerate(lines):
            if start_re.search(line):
                start_index = index
                break

        candidate_lines = lines[start_index:]
        if not candidate_lines:
            return ""

        field_re = re.compile(
            r"^(?:【)?(?:学员信息|学员|年级性别|性别年级科目|信息|地址|地点|家庭地址（要有小区名字）|"
            r"学生年级|学生性别|辅导科目|科目|补习时间|时间|时间安排|每周几次|几点到几点|"
            r"平时一周次数|一次上课时长|课时费|薪酬|薪资|薪水|课酬|提供时薪|"
            r"老师要求|教员要求|教师要求|要求|其他备注)(?:】)?\s*[:：]"
        )
        tail_noise_re = re.compile(
            r"^(?:#|END$|淼淼家教$|微信号[:：]|诚意回收家教单|入群|学生证|学历证明|管理员只看|"
            r"预览时标签不可点|方示了各种不同的教学方法和|展示了各种不同的教学方法和)"
        )

        end_index = len(candidate_lines)
        seen_field_count = 0
        for index, line in enumerate(candidate_lines):
            if field_re.search(line):
                seen_field_count += 1

            # 在识别到有效信息后，遇到标签/页脚文案即截断，避免污染最后一条 content_block。
            if seen_field_count >= 2 and tail_noise_re.search(line):
                end_index = index
                break

        return "\n".join(candidate_lines[:end_index]).strip()

    @staticmethod
    def _is_meaningful(item: dict) -> bool:
        key_fields = [
            "grade",
            "subject",
            "address",
            "time_schedule",
            "salary_text",
            "teacher_requirement",
        ]
        return any(item.get(field, "") for field in key_fields)

    @staticmethod
    def _extract_first(text: str, rules: list[str]) -> tuple[str, str]:
        for rule in rules:
            match = re.search(rule, text, flags=re.IGNORECASE)
            if not match:
                continue
            value = (match.group(1) or "").strip()
            snippet = match.group(0).strip()
            return value, snippet
        return "", ""

    @staticmethod
    def _normalize_location_fields(result: dict) -> None:
        city = result.get("city", "")
        district = result.get("district", "")

        if city and not district:
            merged = re.match(
                r"^([\u4e00-\u9fa5]{2,6}?)([\u4e00-\u9fa5]{1,8}(?:区|县|市))$",
                city,
            )
            if merged:
                result["city"] = merged.group(1)
                result["district"] = merged.group(2)

        if not result.get("district"):
            address = result.get("address", "")
            district_match = re.search(r"^([\u4e00-\u9fa5]{1,6}(?:区|县|市))", address)
            if district_match:
                result["district"] = district_match.group(1)

    @staticmethod
    def _infer_city_from_article(article: dict, result: dict) -> None:
        if result.get("city"):
            return

        title = article.get("title", "")
        text = article.get("content_text", "")
        candidates = [title, text[:400]]
        for source in candidates:
            # 仅在标题样式标识（开头、空白或标签符号后）推断城市，避免误匹配“包含家教”等描述文本。
            city_match = re.search(r"(?:^|[\s#【\[])([\u4e00-\u9fa5]{2,8})家教", source)
            if city_match:
                result["city"] = city_match.group(1)
                return

    @staticmethod
    def _normalize_text_fields(result: dict) -> None:
        for field in [
            "city",
            "district",
            "grade",
            "subject",
            "address",
            "time_schedule",
            "salary_text",
            "teacher_requirement",
        ]:
            value = result.get(field, "")
            value = value.strip(" ：:;；，,。[]【】")
            result[field] = value

        time_value = result.get("time_schedule", "")
        if time_value:
            has_time_keyword = re.search(
                r"(周|月|天|寒假|暑假|开学|开始|下午|上午|晚上|每次|一次)",
                time_value,
            )
            looks_like_salary = re.search(
                r"(时薪|薪|\d+\s*/\s*\d+|\d+\s*(?:元|h|H))",
                time_value,
                flags=re.IGNORECASE,
            )
            if looks_like_salary and not has_time_keyword:
                result["time_schedule"] = ""
                result["time_schedule_snippet"] = ""

    @staticmethod
    def _fill_from_compound_lines(text: str, result: dict) -> None:
        lines = [line.strip() for line in text.split("\n") if line.strip()]
        grade_re = re.compile(
            r"(幼儿(?:小班|中班|大班)?|[一二三四五六七八九十]{1,2}年级|初[一二三]|高[一二三])"
        )
        subject_kw_re = re.compile(
            r"(全科|数学|英语|语文|物理|化学|科学|生物|历史|地理|政治|奥数|口语|KET|编程|表演)"
        )

        for line in lines:
            if "性别年级科目" in line:
                payload = re.split(r"[:：]", line, maxsplit=1)
                payload_text = payload[1].strip() if len(payload) > 1 else ""
                grade_match = grade_re.search(payload_text)

                if not result.get("grade") and grade_match:
                    result["grade"] = grade_match.group(1)
                    if not result.get("grade_snippet"):
                        result["grade_snippet"] = line

                if not result.get("subject"):
                    subject_text = payload_text
                    if grade_match:
                        subject_text = payload_text[grade_match.end() :]
                    subject_text = re.sub(r"^[男女]\s*", "", subject_text)
                    subject_text = subject_text.strip(" ：:，,；;。")
                    if subject_text:
                        result["subject"] = subject_text
                        if not result.get("subject_snippet"):
                            result["subject_snippet"] = line

            if line.startswith("信息"):
                payload = re.split(r"[:：]", line, maxsplit=1)
                payload_text = payload[1].strip() if len(payload) > 1 else ""
                parts = [
                    part.strip()
                    for part in re.split(r"[，,；;]", payload_text)
                    if part.strip()
                ]

                if not result.get("grade") and parts and grade_re.search(parts[0]):
                    result["grade"] = parts[0]
                    if not result.get("grade_snippet"):
                        result["grade_snippet"] = line

                if not result.get("subject"):
                    for part in parts:
                        if subject_kw_re.search(part):
                            result["subject"] = part
                            if not result.get("subject_snippet"):
                                result["subject_snippet"] = line
                            break

                if not result.get("time_schedule"):
                    for part in parts:
                        if re.search(
                            r"(周|寒假|开学|开始|一次|每次|下午|上午|晚上)", part
                        ):
                            result["time_schedule"] = part
                            if not result.get("time_schedule_snippet"):
                                result["time_schedule_snippet"] = line
                            break

                if not result.get("salary_text"):
                    for part in parts:
                        if re.search(
                            r"(\d+\s*/\s*\d+|\d+\s*(?:元|h|H)|报价|面议)", part
                        ):
                            result["salary_text"] = part
                            if not result.get("salary_snippet"):
                                result["salary_snippet"] = line
                            break

            if ":" not in line and "：" not in line:
                if not result.get("grade"):
                    grade_match = grade_re.search(line)
                    if grade_match:
                        result["grade"] = grade_match.group(1)
                        if not result.get("grade_snippet"):
                            result["grade_snippet"] = line

                if not result.get("subject"):
                    subject_match = subject_kw_re.search(line)
                    if subject_match:
                        result["subject"] = subject_match.group(1)
                        if not result.get("subject_snippet"):
                            result["subject_snippet"] = line
