package com.lin.webtemplate;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.lin.webtemplate.service.config.AdminAuthJwtProperties;
import com.lin.webtemplate.service.model.PythonCommandExecutionResultModel;
import com.lin.webtemplate.service.service.AdminAuthService;
import com.lin.webtemplate.service.service.PythonCrawlerCommandService;

/**
 * 功能：验证后台导入接口的平台查询、校验与去重行为。
 *
 * @author linyi
 * @since 2026-02-19
 */
@SpringBootTest(classes = WebTemplateApplication.class)
@AutoConfigureMockMvc
class AdminContentImportControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AdminAuthService adminAuthService;

    @Resource
    private AdminAuthJwtProperties adminAuthJwtProperties;

    @MockBean
    private PythonCrawlerCommandService pythonCrawlerCommandService;

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
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS crawl_platform (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    platform_code VARCHAR(64) NOT NULL,
                    platform_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    description VARCHAR(512) NOT NULL,
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
        jdbcTemplate.update("DELETE FROM crawl_platform");
        jdbcTemplate.update("DELETE FROM crawl_task_log");
        jdbcTemplate.update("DELETE FROM crawl_task");

        adminAuthService.initOrRotateAdmin("admin", "admin123456", "ENABLED");
        jdbcTemplate.update(
                "INSERT INTO crawl_platform(platform_code, platform_name, status, description) VALUES (?, ?, ?, ?)",
                "MIAOMIAO_WECHAT", "淼淼家教公众号", "ENABLED", "默认平台"
        );
        jdbcTemplate.update(
                "INSERT INTO crawl_platform(platform_code, platform_name, status, description) VALUES (?, ?, ?, ?)",
                "LEGACY_WECHAT", "旧平台", "DISABLED", "禁用平台"
        );

        PythonCommandExecutionResultModel executionResultModel = new PythonCommandExecutionResultModel();
        executionResultModel.setSuccess(true);
        executionResultModel.setExitCode(0);
        given(pythonCrawlerCommandService.executeImportTask(anyLong(), anyBoolean())).willReturn(executionResultModel);
    }

    @Test
    void platformOptions_shouldReturnEnabledPlatformsOnly() throws Exception {
        mockMvc.perform(get("/admin/import/platform-options").cookie(loginCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].platformCode").value("MIAOMIAO_WECHAT"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void importTasks_shouldRejectWhenExceedingLimit() throws Exception {
        mockMvc.perform(post("/admin/import/tasks")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformCode": "MIAOMIAO_WECHAT",
                                  "urls": [
                                    "https://example.com/1",
                                    "https://example.com/2",
                                    "https://example.com/3",
                                    "https://example.com/4",
                                    "https://example.com/5",
                                    "https://example.com/6",
                                    "https://example.com/7",
                                    "https://example.com/8",
                                    "https://example.com/9",
                                    "https://example.com/10",
                                    "https://example.com/11"
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IMPORT_LIMIT_EXCEEDED"));
    }

    @Test
    void importTasks_shouldDeduplicateUrlsAndReturnAcceptanceSummary() throws Exception {
        mockMvc.perform(post("/admin/import/tasks")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformCode": "MIAOMIAO_WECHAT",
                                  "urls": [
                                    "https://example.com/a",
                                    "https://example.com/a",
                                    "https://example.com/b"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.submittedCount").value(3))
                .andExpect(jsonPath("$.data.acceptedCount").value(2))
                .andExpect(jsonPath("$.data.deduplicatedCount").value(1))
                .andExpect(jsonPath("$.data.taskIds.length()").value(2));
    }

    @Test
    void importTaskProgress_shouldReturnSummaryAndLatestFailureMessage() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO crawl_task(id, source_url, platform_code, source_type, status) VALUES (?, ?, ?, ?, ?)",
                101L, "https://example.com/pending", "MIAOMIAO_WECHAT", "MANUAL", "PENDING"
        );
        jdbcTemplate.update(
                "INSERT INTO crawl_task(id, source_url, platform_code, source_type, status) VALUES (?, ?, ?, ?, ?)",
                102L, "https://example.com/failed", "MIAOMIAO_WECHAT", "MANUAL", "FAILED"
        );
        jdbcTemplate.update(
                "INSERT INTO crawl_task_log(task_id, runtime, stage, status, error_type, error_summary, error_message) VALUES (?, ?, ?, ?, ?, ?, ?)",
                102L, "python", "PYTHON_EXECUTE", "FAILED", "EMPTY_PARSED_RESULT", "exitCode=2", "解析失败：no meaningful parsed fields"
        );

        mockMvc.perform(post("/admin/import/tasks/progress")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskIds\":[101,102]}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.finishedCount").value(1))
                .andExpect(jsonPath("$.data.allFinished").value(false))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[1].latestRuntime").value("python"))
                .andExpect(jsonPath("$.data.items[1].latestStage").value("PYTHON_EXECUTE"))
                .andExpect(jsonPath("$.data.items[1].latestErrorType").value("EMPTY_PARSED_RESULT"))
                .andExpect(jsonPath("$.data.items[1].latestErrorSummary").value("exitCode=2"));
    }

    @Test
    void importTasks_shouldNotExecuteAgainWhenTaskAlreadySuccess() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO crawl_task(id, source_url, platform_code, source_type, status) VALUES (?, ?, ?, ?, ?)",
                201L, "https://example.com/done", "MIAOMIAO_WECHAT", "MANUAL", "SUCCESS"
        );
        given(pythonCrawlerCommandService.createOrReuseTask(anyString(), anyString(), anyString()))
                .willReturn(201L);

        mockMvc.perform(post("/admin/import/tasks")
                        .cookie(loginCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformCode": "MIAOMIAO_WECHAT",
                                  "urls": [
                                    "https://example.com/done"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(pythonCrawlerCommandService, after(300).never())
                .executeImportTask(eq(201L), anyBoolean());
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
