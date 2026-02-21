package com.lin.webtemplate.service.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.lin.webtemplate.service.config.AdminInitProperties;

/**
 * 功能：管理员账号初始化运行器（运维入口）。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Slf4j
@Component
public class AdminUserInitRunner implements ApplicationRunner {

    @Resource
    private AdminInitProperties adminInitProperties;

    @Resource
    private AdminAuthService adminAuthService;

    @Override
    public void run(ApplicationArguments args) {
        if (!adminInitProperties.isEnabled()) {
            return;
        }
        adminAuthService.initOrRotateAdmin(
                adminInitProperties.getUsername(),
                adminInitProperties.getPassword(),
                adminInitProperties.getStatus());
        log.info("AdminUserInitRunner finished, username={}", adminInitProperties.getUsername());
    }
}
