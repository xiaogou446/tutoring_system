package com.lin.webtemplate.service.model;

import java.util.List;

import lombok.Data;

/**
 * 功能：后台日志查询结果，统一承载 Java 与 Python 运行日志。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Data
public class AdminRuntimeLogQueryResultModel {

    private String runtime;

    private Integer totalCount;

    private List<AdminRuntimeLogItemModel> items;
}
