package com.lin.webtemplate.service.model;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：H5 家教列表单条数据模型。
 *
 * 该模型遵循最小返回集，只提供列表展示所需字段，
 * 用于降低前端首屏接口负载与解析复杂度。
 *
 * @author linyi
 * @since 2026-02-17
 */
@Data
public class TutoringInfoItemModel {

    /** 家教信息主键ID。 */
    private Long id;

    /** 家教信息来源URL（含 item 后缀）。 */
    private String sourceUrl;

    /** 家教信息完整原文分段。 */
    private String contentBlock;

    /** 原文发布时间。 */
    private LocalDateTime publishedAt;
}
