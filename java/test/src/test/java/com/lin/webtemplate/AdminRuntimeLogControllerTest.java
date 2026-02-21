package com.lin.webtemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.service.AdminAuthService;

/**
 * 功能：验证后台运行日志查询接口的筛选能力与鉴权行为。
 *
 * @author linyi
 * @since 2026-02-21
 */
@SpringBootTest(classes = WebTemplateApplication.class)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "admin.runtime-log.java-log-dir=target/runtime-log-test/java",
        "admin.runtime-log.java-file-prefix=application",
        "admin.runtime-log.max-query-lines=200",
        "admin.runtime-log.retention-days=30"
})
class AdminRuntimeLogControllerTest {

    private static final Path JAVA_LOG_DIR = Path.of("target/runtime-log-test/java");

    @Resource
    private MockMvc mockMvc;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AdminAuthService adminAuthService;

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    @BeforeEach
    void setUp() throws Exception {
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
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS crawl_task_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    runtime VARCHAR(16) NOT NULL DEFAULT 'python',
                    stage VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    error_type VARCHAR(64) NOT NULL,
                    error_summary VARCHAR(256) NOT NULL DEFAULT '',
                    error_message VARCHAR(512) NOT NULL,
                    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    finished_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        addColumnIfMissing(
                "crawl_task_log",
                "runtime",
                "ALTER TABLE crawl_task_log ADD COLUMN runtime VARCHAR(16) NOT NULL DEFAULT 'python'"
        );
        addColumnIfMissing(
                "crawl_task_log",
                "error_summary",
                "ALTER TABLE crawl_task_log ADD COLUMN error_summary VARCHAR(256) NOT NULL DEFAULT ''"
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS crawl_task (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    source_url VARCHAR(1024) NOT NULL,
                    platform_code VARCHAR(64) NOT NULL,
                    source_type VARCHAR(32) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        jdbcTemplate.update("DELETE FROM admin_user");
        jdbcTemplate.update("DELETE FROM crawl_task_log");
        jdbcTemplate.update("DELETE FROM crawl_task");
        adminAuthService.initOrRotateAdmin("admin", "admin123456", "ENABLED");

        deleteDirectory(JAVA_LOG_DIR);
        Files.createDirectories(JAVA_LOG_DIR);
    }

    @Test
    void queryRuntimeLogs_shouldFilterJavaLogsByKeywordTimeAndLevel() throws Exception {
        LocalDateTime base = LocalDateTime.now().minusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String infoLine = formatter.format(base) + " INFO  c.l.s.TestLogger - java info line";
        String errorLine = formatter.format(base.plusMinutes(1))
                + " ERROR c.l.s.TestLogger - java parse failed: keyword-hit";
        Files.writeString(
                JAVA_LOG_DIR.resolve("application.log"),
                infoLine + System.lineSeparator() + errorLine + System.lineSeparator(),
                StandardCharsets.UTF_8
        );

        String body = """
                {
                  "runtime": "java",
                  "level": "ERROR",
                  "keyword": "keyword-hit",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(base.minusMinutes(1), base.plusMinutes(2));

        mockMvc.perform(post("/admin/runtime-logs/query")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runtime").value("java"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].level").value("ERROR"))
                .andExpect(jsonPath("$.data.items[0].message").value("java parse failed: keyword-hit"));
    }

    @Test
    void queryRuntimeLogs_shouldReturnPythonTaskLogsWithUnifiedFormat() throws Exception {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(2);
        jdbcTemplate.update(
                "INSERT INTO crawl_task(id, source_url, platform_code, source_type, status) VALUES (?, ?, ?, ?, ?)",
                3001L, "https://example.com/log-python", "MIAOMIAO_WECHAT", "MANUAL", "FAILED"
        );
        jdbcTemplate.update(
                "INSERT INTO crawl_task_log(task_id, runtime, stage, status, error_type, error_summary, error_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                3001L, "python", "PYTHON_EXECUTE", "FAILED", "EMPTY_PARSED_RESULT", "exitCode=2", "keyword-hit", createdAt
        );

        String body = """
                {
                  "runtime": "python",
                  "keyword": "keyword-hit",
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(createdAt.minusMinutes(1), createdAt.plusMinutes(1));

        mockMvc.perform(post("/admin/runtime-logs/query")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runtime").value("python"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].runtime").value("python"))
                .andExpect(jsonPath("$.data.items[0].level").value("ERROR"))
                .andExpect(jsonPath("$.data.items[0].message").value("taskId=3001 runtime=python stage=PYTHON_EXECUTE status=FAILED errorType=EMPTY_PARSED_RESULT errorSummary=exitCode=2 errorMessage=keyword-hit"));
    }

    @Test
    void queryRuntimeLogs_withUnknownRuntime_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/admin/runtime-logs/query")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runtime": "ruby"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RUNTIME_INVALID"));
    }

    @Test
    void queryRuntimeLogs_shouldWriteControllerAndServiceLog(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/admin/runtime-logs/query")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runtime": "java",
                                  "limit": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String content = output.getOut();
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("AdminRuntimeLogController.query start"));
        org.junit.jupiter.api.Assertions.assertTrue(content.contains("AdminRuntimeLogService.query done"));
    }

    private Cookie loginCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return result.getResponse().getCookie(adminAuthJwtProperties.getCookieName());
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

    private void addColumnIfMissing(String tableName,
                                    String columnName,
                                    String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
