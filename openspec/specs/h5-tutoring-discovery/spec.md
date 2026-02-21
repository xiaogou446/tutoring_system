## Purpose

定义面向前端检索展示的分页查询与筛选排序能力，确保接口契约稳定并可被 Web/H5 复用。

## Requirements

### Requirement: 分页查询接口默认按发布时间倒序
系统 MUST 提供 `GET /h5/tutoring-info/page` 分页接口，并默认按发布时间倒序展示记录。

#### Scenario: 使用默认参数查询
- **WHEN** 调用方未传 `sortOrder` 或传入非法值
- **THEN** 系统按发布时间 `DESC` 返回分页结果

### Requirement: 多字段关键词筛选
系统 SHALL 支持按查询参数对多字段执行模糊筛选，至少包含内容、年级、科目、地址、时间、薪资、老师要求。

#### Scenario: 组合传入多个关键词
- **WHEN** 调用方同时传入 `subjectKeyword=英语` 且 `addressKeyword=滨江`
- **THEN** 系统仅返回同时满足所有筛选条件的记录

### Requirement: 排序仅支持发布时间升降序
系统 MUST 支持 `sortOrder=desc|asc` 两种排序，且均基于发布时间字段。

#### Scenario: 指定升序排序
- **WHEN** 调用方传入 `sortOrder=asc`
- **THEN** 系统按发布时间从早到晚返回结果

### Requirement: 列表返回最小展示字段
系统 SHALL 在分页记录中返回 `id`、`sourceUrl`、`contentBlock`、`publishedAt`，满足列表展示与原文跳转。

#### Scenario: 前端列表展示与跳转
- **WHEN** 前端收到分页数据
- **THEN** 前端可直接渲染内容并使用 `sourceUrl` 跳转原文，无需独立详情接口

### Requirement: Web 页面筛选交互对齐接口参数
系统 MUST 提供 Web 页面筛选与分页交互，参数命名与后端接口保持一致。

#### Scenario: 用户在筛选表单按回车
- **WHEN** 用户填写筛选条件后按回车提交
- **THEN** 前端重置为第一页并请求 `/h5/tutoring-info/page`，请求参数包含当前筛选值
