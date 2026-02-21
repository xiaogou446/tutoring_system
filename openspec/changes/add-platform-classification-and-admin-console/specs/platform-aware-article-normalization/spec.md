## ADDED Requirements

### Requirement: 按平台路由解析规则
系统 MUST 基于 `platformCode` 选择对应的平台解析器处理原始文章内容。

#### Scenario: 指定平台执行解析
- **WHEN** 任务携带 `platformCode=XXX`
- **THEN** 系统使用该平台绑定的解析规则生成标准化结果

### Requirement: article_raw 统一输出规范化文本
系统 MUST 在 `article_raw` 层统一产出规范化 `content_text`，并保留原始内容与平台编码。

#### Scenario: 不同平台文章入库
- **WHEN** 两篇来自不同平台的文章被处理
- **THEN** 两条 `article_raw` 记录均包含各自 `platformCode` 且 `content_text` 满足统一下游消费格式

### Requirement: 未支持平台的失败处理
系统 MUST 在遇到未注册或未实现解析器的平台时安全失败，并写入任务失败原因。

#### Scenario: 平台解析器缺失
- **WHEN** 任务平台编码存在但无可用解析实现
- **THEN** 系统将任务标记为失败并记录可读错误信息，且不写入错误格式的标准化内容
