## Why

当前采集链路默认按单一公众号（如“淼淼家教”）内容格式处理，平台扩展能力不足；同时缺少后台管理入口，无法通过页面化方式完成导入、运维日志查看与管理员登录。随着后续接入更多家教信息来源，必须先建立“平台分类 + 可运营后台”的基础能力，避免解析规则与运维流程持续耦合在脚本层。

## What Changes

- 新增平台分类能力：引入平台类型主数据表，用统一 `platformCode` 管理不同来源平台。
- 调整原文入库模型：`article_raw` 增加平台字段，采集/补采流程在入库时绑定来源平台。
- 调整解析编排：按 `platformCode` 路由解析器，先在 `article_raw` 统一产出标准化 `content_text`，保持 `tutoring_info` 生成流程不变。
- 新增后台管理能力（Java + web）：
  - 页面化导入：可选择平台并批量提交页面 URL 列表。
  - 运行日志页签：查看 Java 服务与 Python crawler 运行日志。
  - 管理员登录：仅登录无注册，账号密码由 DB 预置。
- 对现有自动采集、手动补采与任务日志能力做兼容升级，保证历史单平台流程可平滑迁移。

## Capabilities

### New Capabilities
- `platform-source-registry`: 平台主数据管理（平台编码、名称、状态、描述）与平台选择约束。
- `platform-aware-article-normalization`: 基于平台编码路由解析规则并统一生成 `article_raw.content_text`。
- `admin-console-auth`: 后台管理员登录鉴权（DB 预置账号密码，无注册流程）。
- `admin-content-import`: 后台页面化导入能力（选择平台 + 提交 URL 列表 + 触发任务）。
- `admin-runtime-log-center`: 后台日志中心（查看 Java 与 crawler 运行日志）。

### Modified Capabilities
- `wechat-article-ingestion`: 从“单公众号固定来源”扩展为“带平台分类的来源入库”，采集记录需携带 `platformCode`。
- `manual-article-backfill`: 从纯 CLI 导入升级为“CLI + 后台页面”双入口，并在任务模型中透传平台信息。
- `tutoring-info-parsing-normalization`: 解析入口改为按平台路由规则/解析器，但输出到 `tutoring_info` 的标准结构保持兼容。
- `collection-task-observability-retry`: 日志查询能力需支持后台统一展示与按运行端（Java/Python）筛选。

## Impact

- 影响数据库：新增平台表、管理员账号表；扩展 `article_raw`（平台编码）及相关索引/约束。
- 影响后端模块：`infrastructure`（DO/Mapper/Repository/DDL）、`service`（平台路由与后台业务编排）、`bootstrap`（管理端配置与日志读取策略）。
- 影响 crawler：补采/采集任务需携带平台信息，解析流程按平台分发。
- 影响前端：新增后台登录与管理页面（导入页、日志页签），与现有 web 工程集成。
- 影响运维与安全：需定义管理员初始账号初始化方式、密码存储策略（加密/哈希）与日志脱敏规则。
