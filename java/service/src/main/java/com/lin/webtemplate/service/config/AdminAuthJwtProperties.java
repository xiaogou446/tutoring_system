package com.lin.webtemplate.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 功能：后台 JWT 与 Cookie 认证配置。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
@ConfigurationProperties(prefix = "admin.auth.jwt")
public class AdminAuthJwtProperties {

    private String secret = "PLEASE_CHANGE_ADMIN_JWT_SECRET";

    private long expireSeconds = 7200;

    private String cookieName = "ADMIN_TOKEN";

    private String cookiePath = "/";

    private boolean cookieSecure = false;
}
