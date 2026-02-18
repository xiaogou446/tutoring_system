package com.lin.webtemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 功能：验证 H5 家教信息分页查询接口的筛选与排序行为。
 *
 * 该测试直接使用内存库写入样例数据，覆盖模糊筛选、分页以及发布时间升降序。
 * 通过 MockMvc 断言统一返回结构，确保前端联调接口契约稳定。
 *
 * @author linyi
 * @since 2026-02-17
 */
@SpringBootTest(classes = WebTemplateApplication.class)
@AutoConfigureMockMvc
class TutoringInfoQueryControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM tutoring_info");

        jdbcTemplate.update(
                """
                INSERT INTO tutoring_info
                (source_url, content_block, grade_snippet, subject_snippet, address_snippet,
                 time_schedule_snippet, salary_snippet, teacher_requirement_snippet, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "https://mp.weixin.qq.com/s/mock-a#item-1",
                "A同学数学辅导，周末上课",
                "六年级",
                "数学",
                "西湖区",
                "周末",
                "120/小时",
                "女老师",
                java.sql.Timestamp.valueOf("2026-02-17 10:00:00")
        );

        jdbcTemplate.update(
                """
                INSERT INTO tutoring_info
                (source_url, content_block, grade_snippet, subject_snippet, address_snippet,
                 time_schedule_snippet, salary_snippet, teacher_requirement_snippet, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "https://mp.weixin.qq.com/s/mock-b#item-1",
                "B同学英语提升，平日晚间",
                "初二",
                "英语",
                "滨江区",
                "周三晚",
                "150/小时",
                "有经验",
                java.sql.Timestamp.valueOf("2026-02-17 12:00:00")
        );

        jdbcTemplate.update(
                """
                INSERT INTO tutoring_info
                (source_url, content_block, grade_snippet, subject_snippet, address_snippet,
                 time_schedule_snippet, salary_snippet, teacher_requirement_snippet, published_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "https://mp.weixin.qq.com/s/mock-c#item-1",
                "C同学物理冲刺，周末下午",
                "高三",
                "物理",
                "拱墅区",
                "周末下午",
                "200/小时",
                "男老师",
                java.sql.Timestamp.valueOf("2026-02-17 09:00:00")
        );
    }

    @Test
    void pageQuery_shouldSupportFuzzyFilterAndDescSort() throws Exception {
        mockMvc.perform(
                        get("/h5/tutoring-info/page")
                                .param("pageNo", "1")
                                .param("pageSize", "10")
                                .param("subjectKeyword", "英语")
                                .param("addressKeyword", "滨江")
                                .param("sortOrder", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].sourceUrl").value("https://mp.weixin.qq.com/s/mock-b#item-1"))
                .andExpect(jsonPath("$.data.records[0].contentBlock").value("B同学英语提升，平日晚间"));
    }

    @Test
    void pageQuery_shouldSupportAscSortByPublishedAt() throws Exception {
        mockMvc.perform(
                        get("/h5/tutoring-info/page")
                                .param("pageNo", "1")
                                .param("pageSize", "2")
                                .param("sortOrder", "asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].sourceUrl").value("https://mp.weixin.qq.com/s/mock-c#item-1"))
                .andExpect(jsonPath("$.data.records[1].sourceUrl").value("https://mp.weixin.qq.com/s/mock-a#item-1"));
    }
}
