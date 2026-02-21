package com.lin.webtemplate.service.dto;

import java.util.List;

import lombok.Data;

/**
 * 功能：后台导入任务进度查询请求参数。
 *
 * @author linyi
 * @since 2026-02-20
 */
@Data
public class AdminImportTaskProgressQueryDTO {

    /** 待查询任务ID列表。 */
    private List<Long> taskIds;
}
