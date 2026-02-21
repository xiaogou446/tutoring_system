package com.lin.webtemplate.service.controller;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lin.webtemplate.facade.model.Result;
import com.lin.webtemplate.service.dto.TutoringInfoPageQueryDTO;
import com.lin.webtemplate.service.model.TutoringInfoPageModel;
import com.lin.webtemplate.service.service.TutoringInfoQueryService;

/**
 * 功能：提供 H5 家教列表分页查询接口。
 *
 * 当前接口支持完整内容与多个证据片段字段的模糊筛选，
 * 并支持按发布时间升降序返回，满足 H5 端列表检索场景。
 *
 * @author linyi
 * @since 2026-02-17
 */
@Slf4j
@RestController
@RequestMapping("/h5/tutoring-info")
public class TutoringInfoController {

    @Resource
    private TutoringInfoQueryService tutoringInfoQueryService;

    @GetMapping("/page")
    public Result<TutoringInfoPageModel> pageQuery(@ModelAttribute TutoringInfoPageQueryDTO queryDTO) {
        log.info("TutoringInfoController.pageQuery start, pageNo={}, pageSize={}, sortOrder={}",
                queryDTO == null ? null : queryDTO.getPageNo(),
                queryDTO == null ? null : queryDTO.getPageSize(),
                queryDTO == null ? null : queryDTO.getSortOrder());
        TutoringInfoPageModel tutoringInfoPageModel = tutoringInfoQueryService.pageQuery(queryDTO);
        log.info("TutoringInfoController.pageQuery done, pageNo={}, pageSize={}, total={}, records={}",
                tutoringInfoPageModel.getPageNo(),
                tutoringInfoPageModel.getPageSize(),
                tutoringInfoPageModel.getTotal(),
                tutoringInfoPageModel.getRecords() == null ? 0 : tutoringInfoPageModel.getRecords().size());
        return Result.ok(tutoringInfoPageModel);
    }
}
