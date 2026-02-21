## ADDED Requirements

### Requirement: 单公众号文章自动发现与增量采集
系统 MUST 对配置的单一目标公众号执行按天监测，并对新文章执行增量采集与原文落库。

#### Scenario: 每日自动监测发现新文章
- **WHEN** 到达预设的每日调度时间
- **THEN** 系统对目标公众号执行文章列表扫描并识别未采集文章

#### Scenario: 发现新文章后完成采集落库
- **WHEN** 系统识别到未采集的文章链接
- **THEN** 系统拉取文章正文并保存原始内容、来源链接与发布时间

### Requirement: 支持列表页 HTML 与 appmsg_list JSON 两种发现源
系统 SHALL 同时支持从普通 HTML 列表与公众号 `appmsg_list` JSON 结构中发现文章链接。

#### Scenario: 列表来源为 appmsg_list JSON
- **WHEN** 采集入口返回 JSON 且文章链接嵌在 `data.html` 字段
- **THEN** 系统可正确提取文章 URL 并继续执行增量采集

### Requirement: 公开页面采集不依赖 token
系统 SHALL 基于公开可访问的公众号文章页面进行采集，不引入账号 token 或授权流程作为前置条件。

#### Scenario: 在无 token 配置时完成采集
- **WHEN** 系统未配置任何公众号访问 token
- **THEN** 系统仍可通过公开页面完成文章采集流程

### Requirement: 采集任务入队与幂等保护
系统 MUST 为每篇待采集文章创建任务记录，并以文章唯一来源标识实现幂等，避免重复入队。

#### Scenario: 重复扫描到同一文章
- **WHEN** 调度过程中再次扫描到已采集文章链接
- **THEN** 系统不重复创建采集任务并保持已有记录不变
