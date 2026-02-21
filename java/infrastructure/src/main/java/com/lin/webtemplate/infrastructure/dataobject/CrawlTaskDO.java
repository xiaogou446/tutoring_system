package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：采集任务主表 DO，统一承载自动采集与手动补采任务。
 *
 * sourceUrl 作为幂等键，配合 sourceType 区分来源。
 * status 由任务执行器推进，记录任务当前状态。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
public class CrawlTaskDO {

    /** 主键ID。 */
    private Long id;

    /** 采集目标URL。 */
    private String sourceUrl;

    /** 平台编码。 */
    private String platformCode;

    /** 来源类型。 */
    private String sourceType;

    /** 任务状态。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
