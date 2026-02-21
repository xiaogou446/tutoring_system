## ADDED Requirements

### Requirement: 管理员账号登录
系统 MUST 支持后台管理员使用数据库预置账号密码登录，不提供注册能力。

#### Scenario: 使用预置账号登录成功
- **WHEN** 管理员输入正确的用户名与密码
- **THEN** 系统返回登录成功并建立已认证会话

### Requirement: 使用 JWT 作为会话令牌
系统 MUST 使用 JWT 作为后台认证令牌，并通过会话 Cookie 传输，不依赖 Redis 进行集中式令牌存储。

#### Scenario: 已登录用户访问受保护接口
- **WHEN** 请求携带有效 JWT Cookie
- **THEN** 系统校验通过并允许访问后台受保护资源

### Requirement: 无效令牌拒绝访问
系统 MUST 对无效、过期或缺失令牌的请求返回未认证错误。

#### Scenario: JWT 过期
- **WHEN** 管理员使用过期 JWT 访问后台接口
- **THEN** 系统拒绝请求并要求重新登录
