package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：采集平台主数据 DO，定义平台编码与可用状态。
 *
 * 用于约束采集任务与文章原文的来源平台，
 * 支撑后台导入页面的平台下拉与配置管理。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Data
public class CrawlPlatformDO {

    /** 主键ID。 */
    private Long id;

    /** 平台编码。 */
    private String platformCode;

    /** 平台名称。 */
    private String platformName;

    /** 启用状态。 */
    private String status;

    /** 平台描述。 */
    private String description;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
