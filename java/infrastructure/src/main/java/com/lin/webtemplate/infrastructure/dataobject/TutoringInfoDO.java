package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：结构化家教信息 DO，用于检索与 H5 展示。
 *
 * MVP 阶段字段以“可检索 + 可回溯”为主：结构化字段提供默认值，
 * 同时保存对应证据片段与 sourceUrl 便于运营核对与规则迭代。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
public class TutoringInfoDO {

    /** 主键ID。 */
    private Long id;

    /** 关联文章ID，未关联时可为0。 */
    private Long articleId;

    /** 来源URL。 */
    private String sourceUrl;

    /** 家教信息原始分段内容。 */
    private String contentBlock;

    /** 城市。 */
    private String city;

    /** 区域。 */
    private String district;

    /** 年级。 */
    private String grade;

    /** 科目。 */
    private String subject;

    /** 授课地址。 */
    private String address;

    /** 授课时间描述。 */
    private String timeSchedule;

    /** 薪资文本。 */
    private String salaryText;

    /** 老师要求描述。 */
    private String teacherRequirement;

    /** 原文发布时间。 */
    private LocalDateTime publishedAt;

    /** 解析时间。 */
    private LocalDateTime parsedAt;

    /** 城市证据片段。 */
    private String citySnippet;

    /** 区域证据片段。 */
    private String districtSnippet;

    /** 年级证据片段。 */
    private String gradeSnippet;

    /** 科目证据片段。 */
    private String subjectSnippet;

    /** 地址证据片段。 */
    private String addressSnippet;

    /** 授课时间证据片段。 */
    private String timeScheduleSnippet;

    /** 薪资证据片段。 */
    private String salarySnippet;

    /** 老师要求证据片段。 */
    private String teacherRequirementSnippet;
}
