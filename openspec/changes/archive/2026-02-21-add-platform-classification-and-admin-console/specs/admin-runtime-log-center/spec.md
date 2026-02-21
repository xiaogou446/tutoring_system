## ADDED Requirements

### Requirement: 运行日志统一展示
系统 MUST 在后台日志页签统一展示 Java 服务日志与 Python crawler 日志。

#### Scenario: 查看双运行端日志
- **WHEN** 管理员打开日志页签
- **THEN** 系统可按 `runtime=java|python` 展示对应日志内容

### Requirement: Java 日志展示范围
系统 MUST 展示 Java 服务中 Slf4j 输出的 `INFO`、`WARN`、`ERROR` 级别日志。

#### Scenario: 查看 Java 错误告警
- **WHEN** 管理员选择 `runtime=java` 并筛选 `ERROR`
- **THEN** 系统返回匹配级别的 Java 运行日志行

### Requirement: Python 日志展示范围
系统 MUST 展示 Python crawler 采集过程日志，覆盖任务执行、解析与异常信息。

#### Scenario: 查看 Python 采集日志
- **WHEN** 管理员选择 `runtime=python`
- **THEN** 系统返回 crawler 采集过程中产生的日志内容

### Requirement: Java 日志按天切分并保留一个月
系统 MUST 对 Java 日志按天生成文件，并仅保留最近 30 天日志用于后台查询。

#### Scenario: 查询超出保留期日志
- **WHEN** 管理员查询 30 天以前的 Java 日志
- **THEN** 系统返回无数据或超期提示，不提供已清理日志内容

### Requirement: 日志筛选能力
系统 MUST 支持按运行端、关键字与时间范围筛选日志内容。

#### Scenario: 按运行端与关键字筛选
- **WHEN** 管理员选择 `runtime=python` 并输入关键字
- **THEN** 系统仅返回匹配条件的 crawler 日志行

### Requirement: 日志仅支持在线查看
系统 MUST 仅提供日志展示能力，不提供日志下载能力。

#### Scenario: 管理员尝试下载日志
- **WHEN** 管理员在日志页查找下载功能
- **THEN** 系统不提供下载入口
