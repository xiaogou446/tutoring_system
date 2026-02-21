package com.lin.webtemplate.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 功能：Python 导入命令执行参数配置。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
@ConfigurationProperties(prefix = "crawler.python-command")
public class PythonCommandProperties {

    private String pythonBin = "python3";

    private String scriptPath = "crawler/import_articles.py";

    private String workingDirectory = ".";

    private int timeoutSeconds = 600;

    private int maxConcurrency = 1;

    private String dbType = "mysql";

    private String sqlitePath = "crawler/crawler.db";

    private String mysqlHost = "127.0.0.1";

    private int mysqlPort = 3306;

    private String mysqlUser = "root";

    private String mysqlPassword = "";

    private String mysqlDatabase = "tutoring_crawler";

    private String parserMode = "llm";

    private String llmConfig = "crawler/llm_config.json";
}
