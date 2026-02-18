package com.lin.webtemplate.service.model;

import java.util.List;

import lombok.Data;

/**
 * 功能：H5 家教列表分页结果模型。
 *
 * 该模型统一封装分页元信息与当前页记录，
 * 便于前端直接渲染列表并处理分页组件。
 *
 * @author linyi
 * @since 2026-02-17
 */
@Data
public class TutoringInfoPageModel {

    /** 当前页码，从 1 开始。 */
    private Integer pageNo;

    /** 每页条数。 */
    private Integer pageSize;

    /** 满足筛选条件的总记录数。 */
    private Long total;

    /** 当前页记录集合。 */
    private List<TutoringInfoItemModel> records;
}
