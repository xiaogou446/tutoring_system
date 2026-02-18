package com.lin.webtemplate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能：Spring Boot 应用入口，承载 Web API 与定时调度。
 *
 * 组件扫描覆盖 com.lin.webtemplate 下的 service/infrastructure bean。
 * MapperScan 指向 infrastructure mapper 包，避免在每个 Mapper 上分散配置。
 *
 * @author linyi
 * @since 2026-02-16
 */
@SpringBootApplication(scanBasePackages = "com.lin.webtemplate")
@MapperScan("com.lin.webtemplate.infrastructure.mapper")
public class WebTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebTemplateApplication.class, args);
    }
}
