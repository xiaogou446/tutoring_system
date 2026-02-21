package com.lin.webtemplate.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.config.AdminInitProperties;
import com.lin.webtemplate.service.config.PythonCommandProperties;

/**
 * 功能：集中启用 ConfigurationProperties 绑定。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Configuration
@EnableConfigurationProperties({WechatIngestionProperties.class, PythonCommandProperties.class,
        AdminAuthJwtProperties.class, AdminInitProperties.class})
public class PropertiesConfig {
}
