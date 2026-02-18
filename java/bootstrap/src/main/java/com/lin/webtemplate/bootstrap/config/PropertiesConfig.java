package com.lin.webtemplate.bootstrap.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 功能：集中启用 ConfigurationProperties 绑定。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Configuration
@EnableConfigurationProperties({WechatIngestionProperties.class})
public class PropertiesConfig {
}
