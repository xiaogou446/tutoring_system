# crawler

用于实现家教信息的 Python 采集与解析链路，当前包含：

- 单来源列表扫描与文章链接发现
- 增量任务创建（同 URL 幂等）
- 文章正文抓取与原文入库（支持 SQLite / MySQL）
- 规则解析与结构化落库（缺失字段容错）
- 支持接入中转 LLM 进行结构化解析（失败自动回退规则）
- 失败任务重试与按日成功率统计
- 每日固定时间自动调度入口（可配置执行时间）

## 安装依赖

```bash
pip3 install pymysql
```

## 快速运行（SQLite）

```bash
python3 crawler/run_crawler.py --db crawler/crawler.db --list-url "https://example.com/list"
```

## 快速运行（MySQL）

先准备数据库（只需一次）：

```sql
CREATE DATABASE IF NOT EXISTS tutoring_crawler DEFAULT CHARACTER SET utf8mb4;
```

再执行：

```bash
python3 crawler/run_crawler.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --list-url "https://example.com/list"
```

可选参数：

- `--include-pattern`：文章 URL 过滤正则
- `--retry-task-id`：对失败任务执行重试

## LLM 解析接入（推荐）

默认解析模式为 `llm`，会优先调用中转 LLM，把 `content_text` 解析为标准字段；若调用失败或返回异常，会自动回退到规则解析。

请在项目内配置文件 `crawler/llm_config.json` 填写中转参数：

```json
{
  "base_url": "https://your-relay.example.com/v1",
  "api_key": "your_api_key",
  "model": "your_model",
  "timeout_seconds": 30
}
```

如需指定其它配置文件路径，可用 `--llm-config`；命令行参数会覆盖配置文件：

```bash
python3 crawler/run_crawler.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --list-url "https://example.com/list" \
  --parser-mode llm \
  --llm-config "crawler/llm_config.json" \
  --llm-base-url "https://your-relay.example.com/v1" \
  --llm-api-key "your_api_key" \
  --llm-model "your_model" \
  --llm-timeout-seconds 120
```

如需仅使用规则解析：

```bash
python3 crawler/run_crawler.py ... --parser-mode rule
```

## 每日自动调度

```bash
python3 crawler/run_crawler.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --list-url "https://example.com/list" \
  --schedule-daily \
  --daily-run-at 08:00 \
  --poll-seconds 30
```

## 手动导入（支持批量）

当你手上有一批文章 URL，需要立即导入并写入 `tutoring_info` 时，可使用：

```bash
python3 crawler/import_articles.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --url "https://mp.weixin.qq.com/s/xxxx1" \
  --url "https://mp.weixin.qq.com/s/xxxx2"
```

也支持文件批量导入（每行一个 URL，支持 `#` 注释行）：

```bash
python3 crawler/import_articles.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --url-file "crawler/urls.txt"
```

如果文章已在 `article_raw`，只想重解析并回写 `tutoring_info`（不重新抓取页面）：

```bash
python3 crawler/import_articles.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --from-article-raw \
  --url "https://mp.weixin.qq.com/s/xxxx"
```

常用参数：

- `--parser-mode llm|rule`：选择解析模式（默认 `llm`）
- `--llm-config`：指定项目内 LLM 配置文件
- `--source-type MANUAL|AUTO`：新建任务来源类型（默认 `MANUAL`）

## 运行测试

```bash
python3 -m unittest crawler/tests/test_crawl_service.py
python3 -m unittest crawler/tests/test_llm_parser.py
```

## MySQL 结果检查

```sql
SELECT id, source_url, status, created_at FROM crawl_task ORDER BY id DESC LIMIT 20;
SELECT id, source_url, title, published_at FROM article_raw ORDER BY id DESC LIMIT 20;
SELECT id, source_url, LEFT(content_block, 200) AS content_block, city, subject, salary_text FROM tutoring_info ORDER BY id DESC LIMIT 20;
SELECT id, task_id, stage, status, error_type FROM crawl_task_log ORDER BY id DESC LIMIT 50;
```
