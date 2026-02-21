package com.lin.webtemplate.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 功能：后台运行日志中心配置，定义日志目录、保留周期与查询上限。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Data
@ConfigurationProperties(prefix = "admin.runtime-log")
public class AdminRuntimeLogProperties {

    private String javaLogDir = "logs/java";

    private String javaFilePrefix = "application";

    private int retentionDays = 30;

    private int maxQueryLines = 200;
}
