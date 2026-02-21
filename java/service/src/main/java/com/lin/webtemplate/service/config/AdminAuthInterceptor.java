package com.lin.webtemplate.service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lin.webtemplate.facade.model.Result;
import com.lin.webtemplate.service.model.AdminSessionModel;
import com.lin.webtemplate.service.service.AdminJwtService;

/**
 * 功能：后台管理接口 JWT 认证拦截器。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ATTR_ADMIN_ID = "ADMIN_ID";

    public static final String REQUEST_ATTR_ADMIN_USERNAME = "ADMIN_USERNAME";

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    @Resource
    private AdminJwtService adminJwtService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = findTokenFromCookie(request, adminAuthJwtProperties.getCookieName());
        if (token == null) {
            writeUnauthorized(response, "TOKEN_MISSING", "未登录或会话已失效，请重新登录");
            return false;
        }

        try {
            AdminSessionModel sessionModel = adminJwtService.verify(token);
            request.setAttribute(REQUEST_ATTR_ADMIN_ID, sessionModel.getUserId());
            request.setAttribute(REQUEST_ATTR_ADMIN_USERNAME, sessionModel.getUsername());
            return true;
        } catch (JWTVerificationException ex) {
            if (adminJwtService.isExpired(ex)) {
                writeUnauthorized(response, "TOKEN_EXPIRED", "登录态已过期，请重新登录");
                return false;
            }
            writeUnauthorized(response, "TOKEN_INVALID", "登录态无效，请重新登录");
            return false;
        }
    }

    private String findTokenFromCookie(HttpServletRequest request,
                                       String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookieName == null || cookieName.isBlank()) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response,
                                   String code,
                                   String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = objectMapper.writeValueAsString(Result.fail(code, message));
        response.getWriter().write(body);
    }
}
