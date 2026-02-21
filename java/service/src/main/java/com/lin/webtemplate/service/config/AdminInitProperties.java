package com.lin.webtemplate.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 功能：管理员初始化参数配置（运维入口）。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
@ConfigurationProperties(prefix = "admin.auth.init")
public class AdminInitProperties {

    private boolean enabled = false;

    private String username = "";

    private String password = "";

    private String status = "ENABLED";
}
