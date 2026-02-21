package com.lin.webtemplate.service.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：后台运行日志查询参数。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Data
public class AdminRuntimeLogQueryDTO {

    private String runtime;

    private String level;

    private String keyword;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer limit;
}
