package com.lin.webtemplate.service.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lin.webtemplate.facade.model.Result;
import com.lin.webtemplate.service.config.AdminAuthInterceptor;
import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.dto.AdminLoginRequestDTO;
import com.lin.webtemplate.service.model.AdminLoginResultModel;
import com.lin.webtemplate.service.model.AdminSessionModel;
import com.lin.webtemplate.service.service.AdminAuthService;

/**
 * 功能：后台管理员登录与会话接口。
 *
 * @author linyi
 * @since 2026-02-19
 */
@RestController
@Slf4j
@RequestMapping("/admin/auth")
public class AdminAuthController {

    @Resource
    private AdminAuthService adminAuthService;

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    @PostMapping("/login")
    public Result<AdminLoginResultModel> login(@RequestBody AdminLoginRequestDTO requestDTO,
                                                HttpServletResponse response) {
        log.info("AdminAuthController.login start, username={}",
                requestDTO == null ? null : requestDTO.getUsername());
        AdminSessionModel sessionModel = adminAuthService.login(requestDTO.getUsername(), requestDTO.getPassword());
        if (sessionModel == null) {
            log.warn("AdminAuthController.login failed, username={}",
                    requestDTO == null ? null : requestDTO.getUsername());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.fail("AUTH_FAILED", "用户名或密码错误，或账号已禁用");
        }

        Cookie cookie = new Cookie(adminAuthJwtProperties.getCookieName(), sessionModel.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(adminAuthJwtProperties.isCookieSecure());
        cookie.setPath(adminAuthJwtProperties.getCookiePath());
        cookie.setMaxAge((int) adminAuthJwtProperties.getExpireSeconds());
        response.addCookie(cookie);

        AdminLoginResultModel resultModel = new AdminLoginResultModel();
        resultModel.setUserId(sessionModel.getUserId());
        resultModel.setUsername(sessionModel.getUsername());
        resultModel.setExpireAtEpochSeconds(sessionModel.getExpireAtEpochSeconds());
        log.info("AdminAuthController.login done, userId={}, username={}",
                resultModel.getUserId(), resultModel.getUsername());
        return Result.ok(resultModel);
    }

    @GetMapping("/profile")
    public Result<AdminLoginResultModel> profile(HttpServletRequest request,
                                                  HttpServletResponse response) {
        Object userId = request.getAttribute(AdminAuthInterceptor.REQUEST_ATTR_ADMIN_ID);
        Object username = request.getAttribute(AdminAuthInterceptor.REQUEST_ATTR_ADMIN_USERNAME);
        if (userId == null || username == null) {
            log.warn("AdminAuthController.profile unauthorized");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return Result.fail("UNAUTHORIZED", "未登录或会话已失效，请重新登录");
        }

        AdminLoginResultModel resultModel = new AdminLoginResultModel();
        resultModel.setUserId((Long) userId);
        resultModel.setUsername((String) username);
        resultModel.setExpireAtEpochSeconds(0L);
        log.info("AdminAuthController.profile done, userId={}, username={}", userId, username);
        return Result.ok(resultModel);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        log.info("AdminAuthController.logout start");
        Cookie cookie = new Cookie(adminAuthJwtProperties.getCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(adminAuthJwtProperties.isCookieSecure());
        cookie.setPath(adminAuthJwtProperties.getCookiePath());
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        log.info("AdminAuthController.logout done");
        return Result.ok(null);
    }
}
