## ADDED Requirements

### Requirement: 任务状态可追踪
系统 MUST 对采集与解析任务记录统一状态，至少覆盖 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。

#### Scenario: 查看任务执行轨迹
- **WHEN** 维护人员查询某个任务
- **THEN** 系统返回任务状态流转时间与当前结果

### Requirement: 失败原因记录
系统 SHALL 在任务失败时记录机器可读的错误类型与可读错误信息，便于定位问题。

#### Scenario: 采集任务执行失败
- **WHEN** 任务在采集或解析阶段抛出异常
- **THEN** 系统保存失败原因并标记任务为 `FAILED`

### Requirement: 失败任务可重试
系统 MUST 支持对失败任务发起重试，并保留原执行记录与重试结果。

#### Scenario: 重试失败任务
- **WHEN** 维护人员对 `FAILED` 任务触发重试
- **THEN** 系统创建重试执行记录并返回新的执行结果状态

### Requirement: 自动与手动任务统一日志
系统 MUST 将自动采集任务与手动补采任务统一写入同一日志与统计口径。

#### Scenario: 统计任务成功率
- **WHEN** 系统按日期统计任务成功率
- **THEN** 统计结果同时包含自动任务与手动任务
