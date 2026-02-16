package com.lin.webtemplate.facade.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 功能：自动扫描请求参数，封装公众号列表页地址与扫描上限。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class AutoScanRequest {

    @NotBlank(message = "listPageUrl 不能为空")
    private String listPageUrl;

    private Integer maxScanCount;

    public String getListPageUrl() {
        return listPageUrl;
    }

    public void setListPageUrl(String listPageUrl) {
        this.listPageUrl = listPageUrl;
    }

    public Integer getMaxScanCount() {
        return maxScanCount;
    }

    public void setMaxScanCount(Integer maxScanCount) {
        this.maxScanCount = maxScanCount;
    }
}
