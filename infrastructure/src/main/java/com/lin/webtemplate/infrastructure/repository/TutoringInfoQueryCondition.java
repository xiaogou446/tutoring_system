package com.lin.webtemplate.infrastructure.repository;

/**
 * 功能：结构化家教信息查询条件，承载关键词、筛选与排序参数。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class TutoringInfoQueryCondition {

    private String keyword;

    private String district;

    private String grade;

    private String subject;

    private String sort;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
