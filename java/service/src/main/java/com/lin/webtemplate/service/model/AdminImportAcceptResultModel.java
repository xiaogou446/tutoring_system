package com.lin.webtemplate.service.model;

import java.util.List;

import lombok.Data;

/**
 * 功能：后台批量导入受理结果。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminImportAcceptResultModel {

    /** 平台编码。 */
    private String platformCode;

    /** 提交条数（去重前）。 */
    private int submittedCount;

    /** 受理条数（去重后）。 */
    private int acceptedCount;

    /** 去重条数。 */
    private int deduplicatedCount;

    /** 受理任务ID列表。 */
    private List<Long> taskIds;
}
