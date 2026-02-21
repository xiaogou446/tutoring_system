# AGENTS.md

面向本仓库的 agent 执行规范（Java + Vue + Python crawler）。

## 0) 语言与文档策略

- 所有 assistant 与用户对话必须使用中文。
- 所有新增项目文档默认使用中文（代码符号、API 名、第三方专有名词可保留原文）。
- 修改英文文档时，优先追加或改写为中文说明，除非外部规范要求英文。

## 1) 仓库结构速览

- 后端：Maven 多模块，Java 17，Spring Boot 3.3.x。
- 模块顺序：`java/facade` → `java/infrastructure` → `java/service` → `java/bootstrap` → `java/test`。
- 启动模块：`java/bootstrap`。
- 主启动类：`java/bootstrap/src/main/java/com/lin/webtemplate/WebTemplateApplication.java`。
- Web 前端：`web/`（Vue3 + Vite）。
- 爬虫：`crawler/`（Python，支持 SQLite/MySQL，含每日调度）。
- 默认后端端口：`8081`。

## 2) 构建、运行、测试命令（从仓库根目录执行）

### 2.1 Java 后端

- 全量打包（跳过测试）：`mvn clean package -DskipTests`
- 全量校验（含测试）：`mvn clean verify`
- 仅构建启动模块及其依赖：`mvn -pl java/bootstrap -am package -DskipTests`
- 启动后端：`mvn -pl java/bootstrap -am spring-boot:run`

### 2.2 Java 测试（重点：单测单例）

- 全量测试：`mvn test`
- 仅执行 `java/test` 模块测试：
  `mvn -pl java/test -am test -Dsurefire.failIfNoSpecifiedTests=false`
- 执行单个测试类（示例）：
  `mvn -pl java/test -am -Dtest=TutoringInfoQueryControllerTest test -Dsurefire.failIfNoSpecifiedTests=false`
- 执行单个测试方法（示例）：
  `mvn -pl java/test -am -Dtest=TutoringInfoQueryControllerTest#pageQuery_shouldSupportFuzzyFilterAndDescSort test -Dsurefire.failIfNoSpecifiedTests=false`
- 若上游模块出现“无匹配测试”提示，保留：
  `-Dsurefire.failIfNoSpecifiedTests=false`

### 2.3 前端（web）

- 安装依赖：`cd web && npm ci`
- 本地开发：`cd web && npm run dev`
- 生产构建：`cd web && npm run build`
- 本地预览构建产物：`cd web && npm run preview`
- 说明：当前 `package.json` 未配置 lint/test 脚本；若新增请同步更新本文件。

### 2.4 Python 爬虫（crawler）

- 安装依赖（当前最小）：`pip3 install pymysql`
- 单次采集（MySQL 示例）：
  `python3 crawler/run_crawler.py --db-type mysql --mysql-host 127.0.0.1 --mysql-port 3306 --mysql-user root --mysql-password '***' --mysql-database tutoring_crawler --list-url "https://example.com/list"`
- 每日调度：在上面基础上追加
  `--schedule-daily --daily-run-at 08:00 --poll-seconds 30`
- 运行全部 crawler 单测：`python3 -m unittest discover crawler/tests`
- 运行单个测试文件：`python3 -m unittest crawler.tests.test_scheduler`
- 运行单个测试方法（示例）：
  `python3 -m unittest crawler.tests.test_scheduler.DailyScanSchedulerTest.test_should_trigger_once_per_day`

## 3) Lint / 格式化 / 质量门禁

- Java 当前未启用 Checkstyle/Spotless/PMD；以 `mvn clean verify` 作为质量门禁。
- 前端当前未配置 ESLint/Prettier；变更后至少执行 `npm run build` 确认可构建。
- Python 当前未配置 ruff/flake8；变更后至少执行受影响测试。

## 4) Java 代码风格（强制）

### 4.1 格式与注释

- 4 空格缩进，UTF-8 编码，文件末尾保留换行。
- 一个文件只放一个顶层 `public class`。
- 新增类必须包含类级 Javadoc：
  - `功能：...`
  - `@author linyi`
  - `@since YYYY-MM-DD`
- 非直观逻辑需加简洁注释（边界条件、业务分支、降级策略）。

### 4.2 Imports

- 禁止通配符导入；优先显式导入。
- 分组顺序：
  1) `java.*` / `javax.*` / `jakarta.*`
  2) 第三方库（`org.*`、`lombok.*` 等）
  3) 项目内（`com.lin.*`）
- 分组间保留一个空行，删除未使用导入。

### 4.3 类型与命名

- 禁止原始类型（raw type）和无意义 `Object` 透传。
- 泛型边界明确，例如 `Result<TutoringInfoVO>`。
- Mapper 层对象命名使用 `DO`；领域层对象命名使用 `Model`。
- 非框架强约束场景，不使用 `Entity` 作为业务对象命名。
- 常量使用 `UPPER_SNAKE_CASE`，方法/字段使用 `camelCase`。

### 4.4 Spring 约定

- 依赖注入统一使用 `@Resource`。
- 非特殊场景不使用构造器注入或 `@Autowired`。
- Controller 返回保持项目既有包装（如 `Result<T>`）。

### 4.5 SQL DDL 约定

- 新增业务字段默认 `NOT NULL`。
- 新增业务字段必须提供 `DEFAULT`。
- 每张表、每个字段都必须有 `COMMENT`。

### 4.6 异常与日志

- 不吞异常；优先抛出带业务语义的错误信息。
- 可恢复错误返回结构化失败（例如 `Result.fail(...)`）。
- 使用 `@Slf4j` 输出结构化日志，严禁打印密钥/密码/令牌。

## 5) 前端与 Python 风格补充

- Vue/JS 遵循现有代码风格：2 空格缩进、组合式 API、语义化 class 命名。
- 样式以现有 CSS 变量体系为主，避免无约束“魔法值”散落。
- Python 按 PEP 8：4 空格缩进，尽量补充类型标注，函数保持单一职责。

## 6) 测试要求

- 修复缺陷时，必须先补/改测试并确保“先失败后通过”。
- 后端接口改动需覆盖 HTTP 层与 JSON 契约（建议 `MockMvc`）。
- crawler 解析逻辑改动需覆盖边界样例（多条拆分、空字段、噪声清理）。
- 若无法在本地跑全量测试，至少运行受影响模块/文件并在说明中标注范围。

## 7) 依赖与模块边界

- 依赖添加在“最窄模块”，不要把业务依赖随意上提到父 POM。
- 保持既有模块依赖方向，不要在 `facade` 引入 `service`。
- Maven 本地仓库必须走全局配置（`~/.m2/repository` 或 `settings.xml`）。
- 禁止在 `.mvn/maven.config` 设置 `maven.repo.local`。

## 8) Agent 工作流建议

- 改代码前先读：目标模块 POM、相关实现、对应测试。
- 优先做最小闭环修改，避免顺手大重构。
- 提交前顺序：受影响单测 → 模块测试 → 必要时全量 `verify`。
- 若行为与测试冲突，不要“绕过”；应同步修正实现与测试并说明原因。

## 9) Cursor / Copilot 规则同步

- 已检查 `.cursorrules`：不存在。
- 已检查 `.cursor/rules/`：不存在。
- 已检查 `.github/copilot-instructions.md`：不存在。
- 若后续新增上述规则文件，需将关键约束同步并优先遵循。
