## MODIFIED Requirements

### Requirement: 任务状态可追踪
系统 MUST 对采集与解析任务记录统一状态，至少覆盖 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`，并记录运行端信息（Java 编排或 Python 执行）。

#### Scenario: 查看任务执行轨迹
- **WHEN** 维护人员查询某个任务
- **THEN** 系统返回任务状态流转时间、当前结果与运行端标识

### Requirement: 失败原因记录
系统 SHALL 在任务失败时记录机器可读错误类型与可读错误信息，并包含触发阶段（Java 调度或 Python 执行）。

#### Scenario: Python 命令执行失败
- **WHEN** Java 触发 Python 命令返回非零退出码
- **THEN** 系统保存失败原因、标准错误输出摘要并标记任务为 `FAILED`

### Requirement: 失败任务可重试
系统 MUST 支持对失败任务发起重试，并保留原执行记录与重试结果。

#### Scenario: 重试失败任务
- **WHEN** 维护人员对 `FAILED` 任务触发重试
- **THEN** 系统创建重试执行记录并返回新的执行结果状态

### Requirement: 自动与手动任务统一日志
系统 MUST 将自动采集任务与手动补采任务统一写入同一日志与统计口径，并支持后台按运行端筛选展示。

#### Scenario: 后台筛选运行日志
- **WHEN** 管理员在日志页签按 `runtime=java|python` 筛选
- **THEN** 系统返回对应运行端日志与关联任务上下文
