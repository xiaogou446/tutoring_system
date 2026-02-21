package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：采集任务执行日志 DO，记录每次执行的状态流转与失败原因。
 *
 * 允许对同一任务产生多条执行记录，用于重试与时间线追踪。
 * errorType/errorMessage 用于机器可读分类与人类定位。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
public class CrawlTaskLogDO {

    /** 主键ID。 */
    private Long id;

    /** 关联任务ID。 */
    private Long taskId;

    /** 运行端（java/python）。 */
    private String runtime;

    /** 任务阶段。 */
    private String stage;

    /** 阶段状态。 */
    private String status;

    /** 错误类型。 */
    private String errorType;

    /** 错误摘要（如退出码、超时等简述）。 */
    private String errorSummary;

    /** 错误信息。 */
    private String errorMessage;

    /** 阶段开始时间。 */
    private LocalDateTime startedAt;

    /** 阶段结束时间。 */
    private LocalDateTime finishedAt;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
