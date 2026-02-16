package com.lin.webtemplate.backfill;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 功能：验证手动 URL 回填采集链路的端到端行为。
 *
 * @author linyi
 * @since 2026-02-16
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManualBackfillControllerTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void manualBackfill_shouldCreateTaskAndStoreResult() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));

        String sourceUrl = mockWebServer.url("/article-1").toString();

        mockMvc.perform(post("/api/backfill/manual")
                        .contentType("application/json")
                        .content("{\"sourceUrl\":\"" + sourceUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").isNumber())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.idempotentHit").value(false));

        mockMvc.perform(get("/api/backfill/tasks/latest").param("sourceUrl", sourceUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceUrl").value(sourceUrl))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.title").value("上海浦东家教信息（数学）"));
    }

    @Test
    void manualBackfill_shouldBeIdempotentForSameUrl() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));

        String sourceUrl = mockWebServer.url("/article-idempotent").toString();

        mockMvc.perform(post("/api/backfill/manual")
                        .contentType("application/json")
                        .content("{\"sourceUrl\":\"" + sourceUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idempotentHit").value(false));

        mockMvc.perform(post("/api/backfill/manual")
                        .contentType("application/json")
                        .content("{\"sourceUrl\":\"" + sourceUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idempotentHit").value(true));
    }

    @Test
    void autoScan_shouldExtractUrlsAndCreateTasks() throws Exception {
        String articleOneUrl = mockWebServer.url("/article-auto-1").toString();
        String articleTwoUrl = mockWebServer.url("/article-auto-2").toString();

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(buildListPageHtml(articleOneUrl, articleTwoUrl, articleOneUrl)));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));

        String listPageUrl = mockWebServer.url("/public-list-1").toString();

        mockMvc.perform(post("/api/backfill/auto/scan")
                        .contentType("application/json")
                        .content("{\"listPageUrl\":\"" + listPageUrl + "\",\"maxScanCount\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.listPageUrl").value(listPageUrl))
                .andExpect(jsonPath("$.data.discoveredCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.duplicateCount").value(0))
                .andExpect(jsonPath("$.data.tasks[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.tasks[1].status").value("SUCCESS"));
    }

    @Test
    void autoScan_shouldSkipExistingSourceUrl() throws Exception {
        String existingArticleUrl = mockWebServer.url("/article-existing").toString();
        String newArticleUrl = mockWebServer.url("/article-new").toString();
        String listPageUrl = mockWebServer.url("/public-list-2").toString();

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));

        mockMvc.perform(post("/api/backfill/manual")
                        .contentType("application/json")
                        .content("{\"sourceUrl\":\"" + existingArticleUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idempotentHit").value(false));

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(buildListPageHtml(existingArticleUrl, newArticleUrl)));
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=UTF-8")
                .setBody(loadFixture("fixtures/manual-article.html")));

        mockMvc.perform(post("/api/backfill/auto/scan")
                        .contentType("application/json")
                        .content("{\"listPageUrl\":\"" + listPageUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discoveredCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.duplicateCount").value(1))
                .andExpect(jsonPath("$.data.tasks[0].sourceUrl").value(existingArticleUrl))
                .andExpect(jsonPath("$.data.tasks[0].idempotentHit").value(true))
                .andExpect(jsonPath("$.data.tasks[1].sourceUrl").value(newArticleUrl))
                .andExpect(jsonPath("$.data.tasks[1].idempotentHit").value(false));
    }

    private String loadFixture(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String buildListPageHtml(String... urls) {
        StringBuilder html = new StringBuilder("<html><body>");
        for (String url : urls) {
            html.append("<a href=\"")
                    .append(url)
                    .append("\">article</a>");
        }
        html.append("</body></html>");
        return html.toString();
    }
}
