package com.lin.webtemplate.bootstrap.config;

import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 功能：单公众号采集配置项绑定（不引入 token）。
 *
 * enabled 用于快速开关定时采集；windowStart/windowEnd 用于限定调度窗口。
 * listPageUrl 为公开页面入口（如公众号历史文章列表页）。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
@ConfigurationProperties(prefix = "wechat.ingestion")
public class WechatIngestionProperties {

    private boolean enabled = false;

    private String listPageUrl;

    private LocalTime windowStart = LocalTime.of(6, 0);

    private LocalTime windowEnd = LocalTime.of(7, 0);
}
