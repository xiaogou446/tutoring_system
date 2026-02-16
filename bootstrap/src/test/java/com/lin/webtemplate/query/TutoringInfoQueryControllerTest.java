package com.lin.webtemplate.query;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import com.lin.webtemplate.infrastructure.repository.ArticleRawRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 功能：验证结构化家教查询 API 的筛选、排序与详情行为。
 *
 * @author linyi
 * @since 2026-02-16
 */
@SpringBootTest
@AutoConfigureMockMvc
class TutoringInfoQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRawRepository articleRawRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTables() {
        jdbcTemplate.execute("DELETE FROM crawl_task_log");
        jdbcTemplate.execute("DELETE FROM crawl_task");
        jdbcTemplate.execute("DELETE FROM tutoring_info");
        jdbcTemplate.execute("DELETE FROM article_raw");
    }

    @Test
    void list_shouldSupportKeywordFilterAndSalarySort() throws Exception {
        String keywordTag = "QTAG" + System.nanoTime();
        seedArticle("https://example.com/query-a-" + System.nanoTime(), "浦东数学家教" + keywordTag, "浦东新区，初中数学，薪资120元/小时，联系电话13800000001", LocalDateTime.of(2026, 2, 10, 10, 0));
        seedArticle("https://example.com/query-b-" + System.nanoTime(), "浦东数学辅导" + keywordTag, "浦东，初二数学，薪资80元/小时，微信abc001", LocalDateTime.of(2026, 2, 11, 10, 0));
        seedArticle("https://example.com/query-c-" + System.nanoTime(), "浦东数学上门" + keywordTag, "浦东，初中数学，薪资面议", LocalDateTime.of(2026, 2, 12, 10, 0));

        mockMvc.perform(get("/api/tutoring/posts")
                        .param("keyword", keywordTag)
                        .param("district", "浦东")
                        .param("sort", "salary_high")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].salary").value("120元/小时"))
                .andExpect(jsonPath("$.data.records[1].salary").value("80元/小时"))
                .andExpect(jsonPath("$.data.records[2].salary").value("面议"));
    }

    @Test
    void detail_shouldReturnStructuredFieldsById() throws Exception {
        String sourceUrl = "https://example.com/detail-" + System.nanoTime();
        seedArticle(sourceUrl, "徐汇英语家教", "徐汇区，高一英语，薪资100元/小时，联系微信teacher-001", LocalDateTime.of(2026, 2, 13, 9, 0));
        Long rawId = articleRawRepository.findBySourceUrl(sourceUrl).getId();

        mockMvc.perform(get("/api/tutoring/posts/{id}", rawId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(rawId))
                .andExpect(jsonPath("$.data.sourceUrl").value(sourceUrl))
                .andExpect(jsonPath("$.data.district").value("徐汇"))
                .andExpect(jsonPath("$.data.subject").value("英语"))
                .andExpect(jsonPath("$.data.salary").value("100元/小时"));
    }

    private void seedArticle(String sourceUrl, String title, String contentText, LocalDateTime publishTime) {
        ArticleRawEntity entity = new ArticleRawEntity();
        entity.setSourceUrl(sourceUrl);
        entity.setTitle(title);
        entity.setPublishTime(publishTime);
        entity.setHtmlContent("<html><body>fixture</body></html>");
        entity.setContentText(contentText);
        entity.setFetchedAt(LocalDateTime.now());
        articleRawRepository.upsert(entity);
    }
}
