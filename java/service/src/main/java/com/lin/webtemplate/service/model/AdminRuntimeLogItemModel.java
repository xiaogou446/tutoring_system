package com.lin.webtemplate.service.model;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：后台日志中心单条日志视图。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Data
public class AdminRuntimeLogItemModel {

    private String runtime;

    private String level;

    private LocalDateTime timestamp;

    private String message;
}
