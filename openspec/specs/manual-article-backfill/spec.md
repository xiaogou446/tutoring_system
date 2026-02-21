## Purpose

定义手动 URL 补采能力，确保自动采集之外可按需补录、重跑与统一任务追踪。

## Requirements

### Requirement: 手动 URL 补采触发
系统 MUST 提供手动补采入口，允许内部维护人员提交公众号文章 URL 并立即触发采集任务。

#### Scenario: 通过 CLI 提交合法文章 URL
- **WHEN** 运维通过 `python3 crawler/import_articles.py --url <文章URL>` 提交合法 URL
- **THEN** 系统立即创建补采任务并进入统一任务流转

### Requirement: 自动与手动任务统一纳管
系统 SHALL 将手动补采任务与自动采集任务纳入同一任务模型、状态机与日志体系。

#### Scenario: 查询任务执行明细
- **WHEN** 维护人员查看某次手动补采执行结果
- **THEN** 系统返回与自动任务一致的状态、错误信息与重试记录字段

### Requirement: 补采幂等控制
系统 MUST 对手动补采执行幂等校验，避免同一文章被重复创建多个并发任务。

#### Scenario: 短时间重复提交同一 URL
- **WHEN** 维护人员连续提交同一篇文章 URL
- **THEN** 系统仅保留一个有效执行任务并返回幂等提示

### Requirement: 支持从 article_raw 重解析
系统 SHALL 支持在不重新抓取页面的前提下，从 `article_raw` 重解析并回写 `tutoring_info`。

#### Scenario: 使用 from-article-raw 模式补采
- **WHEN** 运维执行 `python3 crawler/import_articles.py --from-article-raw --url <文章URL>`
- **THEN** 系统基于 `article_raw` 记录执行解析并更新结构化结果
