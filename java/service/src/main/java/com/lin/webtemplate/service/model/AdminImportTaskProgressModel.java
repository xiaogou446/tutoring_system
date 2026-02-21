package com.lin.webtemplate.service.model;

import java.util.List;

import lombok.Data;

/**
 * 功能：后台导入任务进度聚合结果。
 *
 * @author linyi
 * @since 2026-02-20
 */
@Data
public class AdminImportTaskProgressModel {

    /** 查询任务总数。 */
    private int totalCount;

    /** 待执行数量。 */
    private int pendingCount;

    /** 执行中数量。 */
    private int runningCount;

    /** 成功数量。 */
    private int successCount;

    /** 失败数量。 */
    private int failedCount;

    /** 已结束数量。 */
    private int finishedCount;

    /** 是否全部结束。 */
    private boolean allFinished;

    /** 任务明细。 */
    private List<AdminImportTaskProgressItemModel> items;
}
