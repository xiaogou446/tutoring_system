package com.lin.webtemplate.service.model;

import lombok.Data;

/**
 * 功能：后台导入页平台下拉选项。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminPlatformOptionModel {

    /** 平台编码。 */
    private String platformCode;

    /** 平台展示名。 */
    private String platformName;
}
