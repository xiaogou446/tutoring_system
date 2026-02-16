package com.lin.webtemplate.facade.dto;

import java.util.List;

/**
 * 功能：结构化家教分页视图，承载记录与分页元数据。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class TutoringInfoPageView {

    private long total;

    private int pageNo;

    private int pageSize;

    private List<TutoringInfoView> records;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<TutoringInfoView> getRecords() {
        return records;
    }

    public void setRecords(List<TutoringInfoView> records) {
        this.records = records;
    }
}
