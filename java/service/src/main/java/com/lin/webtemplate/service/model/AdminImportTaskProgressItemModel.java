package com.lin.webtemplate.service.model;

import lombok.Data;

/**
 * 功能：后台导入任务单条进度项。
 *
 * @author linyi
 * @since 2026-02-20
 */
@Data
public class AdminImportTaskProgressItemModel {

    /** 任务ID。 */
    private Long taskId;

    /** 任务URL。 */
    private String sourceUrl;

    /** 任务状态。 */
    private String status;

    /** 是否已完成（SUCCESS/FAILED/NOT_FOUND）。 */
    private boolean finished;

    /** 最新阶段。 */
    private String latestStage;

    /** 最新运行端（java/python）。 */
    private String latestRuntime;

    /** 最新错误类型。 */
    private String latestErrorType;

    /** 最新错误摘要。 */
    private String latestErrorSummary;

    /** 最新错误信息。 */
    private String latestErrorMessage;
}
