package com.lin.webtemplate.service.service;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.lin.webtemplate.infrastructure.dataobject.AdminUserDO;
import com.lin.webtemplate.infrastructure.repository.AdminUserRepository;
import com.lin.webtemplate.service.model.AdminSessionModel;

/**
 * 功能：管理员账号认证服务。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Service
@Slf4j
public class AdminAuthService {

    @Resource
    private AdminUserRepository adminUserRepository;

    @Resource
    private AdminPasswordHashService adminPasswordHashService;

    @Resource
    private AdminJwtService adminJwtService;

    public AdminSessionModel login(String username,
                                   String password) {
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(password);
        log.info("AdminAuthService.login start, username={}", normalizedUsername);
        if (normalizedUsername == null || normalizedPassword == null) {
            log.warn("AdminAuthService.login failed, reason=EMPTY_CREDENTIAL");
            return null;
        }

        AdminUserDO adminUserDO = adminUserRepository.findByUsername(normalizedUsername);
        if (adminUserDO == null) {
            log.warn("AdminAuthService.login failed, username={}, reason=USER_NOT_FOUND", normalizedUsername);
            return null;
        }
        if (!"ENABLED".equals(adminUserDO.getStatus())) {
            log.warn("AdminAuthService.login failed, username={}, reason=USER_DISABLED", normalizedUsername);
            return null;
        }
        if (!adminPasswordHashService.matches(normalizedPassword, adminUserDO.getPasswordHash())) {
            log.warn("AdminAuthService.login failed, username={}, reason=PASSWORD_MISMATCH", normalizedUsername);
            return null;
        }

        adminUserRepository.updateLastLoginAt(adminUserDO.getId());
        AdminSessionModel sessionModel = adminJwtService.issue(adminUserDO);
        log.info("AdminAuthService.login done, userId={}, username={}", adminUserDO.getId(), normalizedUsername);
        return sessionModel;
    }

    public void initOrRotateAdmin(String username,
                                  String rawPassword,
                                  String status) {
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(rawPassword);
        if (normalizedUsername == null || normalizedPassword == null) {
            throw new IllegalArgumentException("admin init username/password can not be blank");
        }
        String normalizedStatus = normalize(status);
        if (normalizedStatus == null) {
            normalizedStatus = "ENABLED";
        }

        String passwordHash = adminPasswordHashService.hash(normalizedPassword);
        AdminUserDO existing = adminUserRepository.findByUsername(normalizedUsername);
        if (existing == null) {
            AdminUserDO adminUserDO = new AdminUserDO();
            adminUserDO.setUsername(normalizedUsername);
            adminUserDO.setPasswordHash(passwordHash);
            adminUserDO.setStatus(normalizedStatus);
            adminUserRepository.insert(adminUserDO);
            return;
        }
        adminUserRepository.updateCredentialsAndStatus(existing.getId(), passwordHash, normalizedStatus);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }
        return trimmedValue;
    }
}
