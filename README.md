# Tutoring System

家教信息采集与运营系统（Java + Vue + Python）。

> 从“公众号文章”到“结构化家教信息”的完整链路：采集、解析、入库、查询、后台导入、日志可观测。

## 项目亮点

- 多平台扩展设计：以 `platform_code` 路由解析器，兼容后续平台接入
- Python 解析双模式：`llm` 优先，失败自动回退 `rule`
- Java 编排护栏：参数化调用、并发上限、超时控制、异常回收
- 后台可运营：登录鉴权、批量导入、任务状态追踪、运行日志查询
- 存储可切换：支持 SQLite / MySQL

## 在线页面（本地启动后）

> 默认前端开发端口 `5173`，后端端口 `8081`。

- H5 查询页：[`http://localhost:5173/`](http://localhost:5173/)
- 后台导入页：[`http://localhost:5173/admin-import.html`](http://localhost:5173/admin-import.html)
- UI 预览页：[`http://localhost:5173/ui-preview.html`](http://localhost:5173/ui-preview.html)
- 后端健康检查（可选自加）：`http://localhost:8081`

## 架构概览

```text
Web(H5/后台) -> Java API(Spring Boot) -> Python Crawler -> DB(MySQL/SQLite)
                       |                     |
                       +---- Runtime Logs ---+
```

仓库结构：

```text
tutoring_system/
├── java/                     # Maven 多模块后端
│   ├── facade/
│   ├── infrastructure/
│   ├── service/
│   ├── bootstrap/            # 启动模块
│   └── test/
├── web/                      # Vue3 + Vite（多页面：index/admin-import）
├── crawler/                  # Python 采集解析链路
├── openspec/                 # 需求/变更规范
└── pom.xml
```

## 快速开始（最短路径）

### 1) 环境准备

- JDK 17+
- Maven 3.9+
- Node.js 18+
- Python 3.10+
- MySQL 8.x（推荐）

### 2) 启动后端

```bash
mvn -pl java/bootstrap -am spring-boot:run
```

### 3) 启动前端

```bash
cd web
npm ci
npm run dev
```

### 4) 运行爬虫（MySQL 示例）

```bash
pip3 install pymysql

python3 crawler/run_crawler.py \
  --db-type mysql \
  --mysql-host 127.0.0.1 \
  --mysql-port 3306 \
  --mysql-user root \
  --mysql-password '你的密码' \
  --mysql-database tutoring_crawler \
  --list-url "https://example.com/list"
```

## 页面截图（占位）

- `docs/screenshots/h5-query.png`（H5 查询页）
- `docs/screenshots/admin-import.png`（后台导入页）
- `docs/screenshots/runtime-logs.png`（日志中心）

> 建议：补图后可在此处直接用 Markdown 图片展示，提高 GitHub 首页可读性。

## API（节选）

- `GET /h5/tutoring-info/page`：家教信息分页查询
- `POST /admin/auth/login`：后台登录
- `GET /admin/auth/profile`：当前管理员信息
- `GET /admin/import/platform-options`：平台选项
- `POST /admin/import/tasks`：批量导入任务（单次最多 10 条）
- `POST /admin/runtime-logs/query`：运行日志查询（`runtime=java|python`）

## 常用命令

```bash
# Java 全量校验（含测试）
mvn clean verify

# Java 单测模块
mvn -pl java/test -am test -Dsurefire.failIfNoSpecifiedTests=false

# Web 构建
cd web && npm run build

# Crawler 测试
python3 -m unittest discover crawler/tests
```

## 关键配置

配置文件：`java/bootstrap/src/main/resources/application.properties`

- 数据源：`spring.datasource.*`
- Java 调 Python：`crawler.python-command.*`
- 后台鉴权：`admin.auth.jwt.*`
- 日志中心：`admin.runtime-log.*`

## 安全提示

- 强烈建议使用独立测试库，避免测试误操作业务数据
- 涉及写库/删库前先备份，并进行二次确认
- 禁止无条件删除业务表数据（如 `DELETE FROM tutoring_info`）

## Roadmap

- 完成 8.x/9.x 回归与发布运维项（OpenSpec 计划）
- 增强日志可观测（traceId / 链路关联）
- 完善回滚手册与部署文档

## 贡献指南

欢迎提交 Issue / PR。提交前建议：

1. 先跑受影响模块测试
2. 避免跨模块大范围重构
3. 保持变更最小闭环并补充必要说明

## License

暂未声明。开源发布建议补充 `LICENSE`（如 MIT / Apache-2.0）。
