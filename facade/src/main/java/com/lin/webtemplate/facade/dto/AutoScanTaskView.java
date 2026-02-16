package com.lin.webtemplate.facade.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能：自动扫描结果视图，返回发现、入队统计与任务明细。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class AutoScanTaskView {

    private String listPageUrl;

    private int discoveredCount;

    private int createdCount;

    private int duplicateCount;

    private List<ManualBackfillTaskView> tasks = new ArrayList<>();

    public String getListPageUrl() {
        return listPageUrl;
    }

    public void setListPageUrl(String listPageUrl) {
        this.listPageUrl = listPageUrl;
    }

    public int getDiscoveredCount() {
        return discoveredCount;
    }

    public void setDiscoveredCount(int discoveredCount) {
        this.discoveredCount = discoveredCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public List<ManualBackfillTaskView> getTasks() {
        return tasks;
    }

    public void setTasks(List<ManualBackfillTaskView> tasks) {
        this.tasks = tasks;
    }
}
