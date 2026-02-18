package com.lin.webtemplate.service.dto;

import lombok.Data;

/**
 * 功能：H5 家教列表分页查询参数 DTO。
 *
 * 该对象承载分页、排序与各类模糊筛选词，
 * 由 Controller 统一接收并下沉到查询服务层处理。
 *
 * @author linyi
 * @since 2026-02-17
 */
@Data
public class TutoringInfoPageQueryDTO {

    /** 页码，从 1 开始。 */
    private Integer pageNo;

    /** 每页条数。 */
    private Integer pageSize;

    /** 发布时间排序方向，支持 asc/desc。 */
    private String sortOrder;

    /** 完整内容模糊筛选词，对应 content_block。 */
    private String contentKeyword;

    /** 年级信息模糊筛选词，对应 grade_snippet。 */
    private String gradeKeyword;

    /** 科目信息模糊筛选词，对应 subject_snippet。 */
    private String subjectKeyword;

    /** 地址信息模糊筛选词，对应 address_snippet。 */
    private String addressKeyword;

    /** 时间信息模糊筛选词，对应 time_schedule_snippet。 */
    private String timeKeyword;

    /** 薪酬信息模糊筛选词，对应 salary_snippet。 */
    private String salaryKeyword;

    /** 教员信息模糊筛选词，对应 teacher_requirement_snippet。 */
    private String teacherKeyword;
}
