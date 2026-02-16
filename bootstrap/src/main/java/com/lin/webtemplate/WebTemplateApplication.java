package com.lin.webtemplate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能：Spring Boot 启动入口并加载 MyBatis Mapper。
 *
 * @author linyi
 * @since 2026-02-16
 */
@SpringBootApplication
@MapperScan("com.lin.webtemplate.infrastructure.mapper")
public class WebTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebTemplateApplication.class, args);
    }
}
