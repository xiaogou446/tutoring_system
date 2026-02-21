package com.lin.webtemplate.service.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lin.webtemplate.facade.model.Result;
import com.lin.webtemplate.service.dto.AdminRuntimeLogQueryDTO;
import com.lin.webtemplate.service.model.AdminRuntimeLogQueryResultModel;
import com.lin.webtemplate.service.service.AdminRuntimeLogService;
import com.lin.webtemplate.service.service.AdminRuntimeLogValidationException;

/**
 * 功能：后台运行日志查询接口，仅提供在线查看能力。
 *
 * @author linyi
 * @since 2026-02-21
 */
@RestController
@Slf4j
@RequestMapping("/admin/runtime-logs")
public class AdminRuntimeLogController {

    @Resource
    private AdminRuntimeLogService adminRuntimeLogService;

    @PostMapping("/query")
    public Result<AdminRuntimeLogQueryResultModel> query(@RequestBody(required = false) AdminRuntimeLogQueryDTO requestDTO,
                                                          HttpServletResponse response) {
        try {
            log.info("AdminRuntimeLogController.query start, runtime={}, level={}, limit={}",
                    requestDTO == null ? null : requestDTO.getRuntime(),
                    requestDTO == null ? null : requestDTO.getLevel(),
                    requestDTO == null ? null : requestDTO.getLimit());
            AdminRuntimeLogQueryResultModel resultModel = adminRuntimeLogService.query(requestDTO);
            log.info("AdminRuntimeLogController.query done, runtime={}, totalCount={}",
                    resultModel.getRuntime(), resultModel.getTotalCount());
            return Result.ok(resultModel);
        } catch (AdminRuntimeLogValidationException ex) {
            log.warn("AdminRuntimeLogController.query validation failed, code={}, message={}",
                    ex.getCode(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Result.fail(ex.getCode(), ex.getMessage());
        }
    }
}
