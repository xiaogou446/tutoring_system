## ADDED Requirements

### Requirement: 后台页面化批量导入
系统 MUST 提供后台导入页面，支持选择平台并提交页面 URL 列表触发导入任务。

#### Scenario: 提交合法平台与 URL 列表
- **WHEN** 管理员选择有效 `platformCode` 并提交 URL 列表
- **THEN** 系统创建导入任务并返回任务受理结果

### Requirement: 批量导入条数上限
系统 MUST 将单次批量导入上限限制为 10 条 URL。

#### Scenario: 提交超过 10 条 URL
- **WHEN** 管理员一次提交 11 条及以上 URL
- **THEN** 系统拒绝请求并返回“单次最多导入 10 条”的错误提示

### Requirement: 导入参数校验与去重
系统 MUST 对导入请求进行 URL 合法性校验与本次请求内去重，避免无效任务进入执行链路。

#### Scenario: 列表中包含重复 URL
- **WHEN** 同一请求中出现重复 URL
- **THEN** 系统去重后按唯一 URL 执行导入并在响应中反馈去重结果

### Requirement: Java 触发 Python 执行的安全约束
系统 MUST 由 Java 后台触发 Python 导入命令，并使用参数化调用、执行超时和并发上限保护。

#### Scenario: Python 导入命令执行超时
- **WHEN** 单次导入执行超过预设超时时间
- **THEN** 系统中止执行并将任务标记为失败，同时记录超时原因
