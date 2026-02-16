package com.lin.webtemplate.facade.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 功能：手动回填请求参数，封装待采集文章 URL。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class ManualBackfillRequest {

    @NotBlank(message = "sourceUrl 不能为空")
    private String sourceUrl;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
