# add-platform-classification-and-admin-console 进展说明

## 1. 目标与当前进度

本变更目标是把现有单平台采集链路升级为“多平台可扩展 + 后台可运营”体系，覆盖：

- 平台主数据与管理员账号模型
- 任务与原文按平台维度隔离（`platform_code`）
- Java 编排 Python 执行链路可观测、可控、可追踪
- 后续后台登录、批量导入、日志中心能力

截至目前：

- 已完成模块：1、2、3、4、5
- 已完成任务：20 项（`1.1~1.4`、`2.1~2.4`、`3.1~3.4`、`4.1~4.4`、`5.1~5.4`）
- 未开始模块：6~9

任务清单见：`openspec/changes/add-platform-classification-and-admin-console/tasks.md`

## 2. 已落地能力（截至模块 3）

### 2.1 数据模型与迁移

- 新增主数据：`crawl_platform`、`admin_user`
- `article_raw`、`crawl_task` 增加 `platform_code`
- 提供回填脚本：`java/bootstrap/src/main/resources/backfill_platform_code.sql`
- 幂等唯一键升级为：`(source_url, platform_code)`

### 2.2 Python 平台路由与解析

- 新增路由层：`platformCode -> parser`（`platform_router.py`）
- 自动采集、手动补采均透传 `platform_code`
- 未支持平台安全失败：写任务失败日志并标记任务失败

### 2.3 Java 调 Python 执行护栏

- 参数化调用（`ProcessBuilder(List<String>)`）
- 并发上限（`Semaphore`）
- 超时控制与强制回收（`destroyForcibly`）
- stdout/stderr/exit code 映射入任务日志

实现位置：

- `java/service/src/main/java/com/lin/webtemplate/service/service/PythonCrawlerCommandService.java`
- `java/service/src/main/java/com/lin/webtemplate/service/config/PythonCommandProperties.java`

### 2.4 后台认证能力（模块 4）

- 新增管理员登录接口：`POST /admin/auth/login`
- 新增受保护示例接口：`GET /admin/auth/profile`
- 使用 JWT + Cookie：登录成功后写入 `ADMIN_TOKEN`（可配置）
- 新增 `/admin/**` 认证拦截器，放行 `/admin/auth/login`
- 登录失败、令牌缺失/过期/无效统一返回未认证错误

运维初始化策略（4.4）：

- 新增启动时初始化入口：`admin.auth.init.*`
- 启用后可“创建或轮换”管理员账号（PBKDF2 哈希存储）

## 3. 任务状态机（重点）

> 说明：`crawl_task_log` 采用“追加日志”模型，不是“更新同一条日志状态”。
> 即每个阶段每次状态变化都会新增一条日志。

### 3.1 task 状态（`crawl_task.status`）

状态集合：

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`

典型迁移：

1. 创建任务：`PENDING`
2. Java 开始执行 Python：`RUNNING`
3. Python 解析完成：
   - 成功 -> `SUCCESS`
   - 失败 -> `FAILED`
4. Java 执行异常（超时/非0退出/IO异常）会直接置 `FAILED`

当前职责分工：

- Java 保证：`RUNNING`、执行异常时 `FAILED`
- Python 保证：业务解析成功时 `SUCCESS`，业务失败时 `FAILED`

### 3.2 task_log 阶段与状态（`crawl_task_log`）

#### A. Java 执行阶段

- `stage=PYTHON_EXECUTE`
  - `RUNNING`：命令开始
  - `SUCCESS`：退出码为 0（附 stdout/stderr 摘要）
  - `FAILED`：并发限制、超时、退出码非0、Java 异常等

常见 `error_type`：

- `JAVA_CONCURRENCY_LIMIT`
- `PYTHON_TIMEOUT`
- `PYTHON_EXIT_NON_ZERO`
- `JAVA_IO_ERROR`
- `JAVA_INTERRUPTED`

#### B. Python 业务阶段

- `stage=FETCH`
  - `RUNNING` / `SUCCESS` / `FAILED`
- `stage=PARSE`
  - `RUNNING` / `SUCCESS` / `FAILED`
- `stage=EXECUTE`
  - `FAILED`（运行期异常）
- `stage=RETRY_N`
  - `RUNNING` / `SUCCESS` / `FAILED`

常见 `error_type`：

- `ARTICLE_NOT_FOUND`
- `UNSUPPORTED_PLATFORM`
- `EMPTY_PARSED_RESULT`
- `RUNTIME_ERROR`
- `RETRY_FAILED`

### 3.3 成功路径时序（示例）

同一 task 下，可能看到如下日志顺序：

1. `PYTHON_EXECUTE RUNNING`（Java）
2. `FETCH ...`（Python）
3. `PARSE ...`（Python）
4. `PYTHON_EXECUTE SUCCESS`（Java）

失败路径会在对应阶段出现 `FAILED`，并可能附带 `PYTHON_EXECUTE FAILED`。

## 4. 幂等规则（截至模块 3）

幂等键定义为：

- `source_url + platform_code`

含义：

- 同一平台同一 URL：不重复建任务
- 不同平台同一 URL：允许分别建任务

已在 SQLite/MySQL 两种存储实现中对齐。

## 5. 配置项（Java -> Python）

配置前缀：`crawler.python-command.*`

关键项：

- `python-bin`
- `script-path`
- `working-directory`
- `timeout-seconds`
- `max-concurrency`
- `db-type`、`mysql-*` / `sqlite-path`
- `parser-mode`、`llm-config`

默认配置见：`java/bootstrap/src/main/resources/application.properties`

## 6. 已执行验证

Python：

- `python3 -m unittest discover crawler/tests`（35 tests, OK）

Java 编译：

- `mvn -pl java/service -am package -DskipTests`（成功）
- `mvn -pl java/bootstrap -am package -DskipTests`（成功）

## 7. 模块 5 完成情况（后台导入页面与接口）

### 7.1 后端接口

- 新增平台下拉接口：`GET /admin/import/platform-options`
- 新增导入受理接口：`POST /admin/import/tasks`
- 导入校验规则：
  - 平台编码必填且必须存在且启用
  - URL 列表必填
  - 单次最多 10 条
  - 每条 URL 必须为合法 `http/https` 地址
  - 请求内重复 URL 自动去重

### 7.2 编排策略

- 受理后按去重 URL 创建/复用 `crawl_task`（`source_type=MANUAL`）
- Java 异步触发 Python 执行（与既有命令护栏兼容）
- 响应中返回 `submittedCount/acceptedCount/deduplicatedCount/taskIds`

### 7.3 后台页面

- 新增独立后台页面：`web/admin-import.html`
- 新增后台入口脚本：`web/src/admin-main.js`
- 新增后台导入页：`web/src/AdminImportApp.vue`
- 新增后台样式：`web/src/admin-style.css`
- 风格与既有 web 页面区分：经典后台布局、白色系极客风、信息密度更高

### 7.4 本模块验证

- `mvn -pl java/test -am -Dtest=AdminContentImportControllerTest test -Dsurefire.failIfNoSpecifiedTests=false`（通过）
- `npm run build`（通过，产出 `dist/admin-import.html`）

## 8. 下一步（模块 6）

按计划进入后台日志中心（`admin-runtime-log-center`）：

- 日志查询接口（`runtime=java|python` + 关键字 + 时间范围）
- Java 日志读取接入（仅查，不支持下载）
- Java 日志按天滚动与 30 天保留策略
- Python crawler 日志读取并统一展示
