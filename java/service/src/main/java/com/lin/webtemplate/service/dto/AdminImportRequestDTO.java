package com.lin.webtemplate.service.dto;

import java.util.List;

import lombok.Data;

/**
 * 功能：后台批量导入请求参数。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminImportRequestDTO {

    /** 平台编码。 */
    private String platformCode;

    /** 待导入 URL 列表。 */
    private List<String> urls;
}
