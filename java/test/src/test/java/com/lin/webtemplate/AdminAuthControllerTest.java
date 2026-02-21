package com.lin.webtemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.service.AdminAuthService;

/**
 * 功能：验证后台管理员登录与 JWT Cookie 鉴权链路。
 *
 * @author linyi
 * @since 2026-02-19
 */
@SpringBootTest(classes = WebTemplateApplication.class)
@AutoConfigureMockMvc
class AdminAuthControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AdminAuthService adminAuthService;

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS admin_user (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(64) NOT NULL,
                    password_hash VARCHAR(256) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    last_login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        jdbcTemplate.update("DELETE FROM admin_user");
        adminAuthService.initOrRotateAdmin("admin", "admin123456", "ENABLED");
    }

    @Test
    void login_shouldSetCookie_andProfileShouldPass() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(adminAuthJwtProperties.getCookieName());
        mockMvc.perform(get("/admin/auth/profile").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void profile_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/auth/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
    }

    @Test
    void profile_withExpiredToken_shouldReturnUnauthorized() throws Exception {
        String expiredToken = JWT.create()
                .withIssuer("tutoring-system-admin")
                .withSubject("1")
                .withClaim("username", "admin")
                .withIssuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .withExpiresAt(Date.from(Instant.now().minusSeconds(3600)))
                .sign(Algorithm.HMAC256(adminAuthJwtProperties.getSecret()));

        mockMvc.perform(get("/admin/auth/profile")
                        .cookie(new MockCookie(adminAuthJwtProperties.getCookieName(), expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void login_withWrongPassword_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void logout_shouldClearCookie_andProfileShouldFailAfterLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie loginCookie = loginResult.getResponse().getCookie(adminAuthJwtProperties.getCookieName());
        MvcResult logoutResult = mockMvc.perform(post("/admin/auth/logout").cookie(loginCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie clearedCookie = logoutResult.getResponse().getCookie(adminAuthJwtProperties.getCookieName());
        mockMvc.perform(get("/admin/auth/profile").cookie(clearedCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TOKEN_MISSING"));
    }
}
