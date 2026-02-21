package com.lin.webtemplate.service.controller;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lin.webtemplate.facade.model.Result;
import com.lin.webtemplate.service.dto.AdminImportTaskProgressQueryDTO;
import com.lin.webtemplate.service.dto.AdminImportRequestDTO;
import com.lin.webtemplate.service.model.AdminImportAcceptResultModel;
import com.lin.webtemplate.service.model.AdminImportTaskProgressModel;
import com.lin.webtemplate.service.model.AdminPlatformOptionModel;
import com.lin.webtemplate.service.service.AdminContentImportService;
import com.lin.webtemplate.service.service.AdminImportValidationException;

/**
 * 功能：后台批量导入接口，提供平台选项查询与导入任务受理。
 *
 * @author linyi
 * @since 2026-02-19
 */
@RestController
@Slf4j
@RequestMapping("/admin/import")
public class AdminImportController {

    @Resource
    private AdminContentImportService adminContentImportService;

    @GetMapping("/platform-options")
    public Result<List<AdminPlatformOptionModel>> listPlatformOptions() {
        log.info("AdminImportController.listPlatformOptions start");
        List<AdminPlatformOptionModel> options = adminContentImportService.listEnabledPlatformOptions();
        log.info("AdminImportController.listPlatformOptions done, optionCount={}", options.size());
        return Result.ok(options);
    }

    @PostMapping("/tasks")
    public Result<AdminImportAcceptResultModel> submitImport(@RequestBody AdminImportRequestDTO requestDTO,
                                                              HttpServletResponse response) {
        try {
            log.info("AdminImportController.submitImport start, platformCode={}, urlCount={}",
                    requestDTO == null ? null : requestDTO.getPlatformCode(),
                    requestDTO == null || requestDTO.getUrls() == null ? 0 : requestDTO.getUrls().size());
            AdminImportAcceptResultModel resultModel = adminContentImportService.acceptImport(
                    requestDTO.getPlatformCode(),
                    requestDTO.getUrls()
            );
            log.info("AdminImportController.submitImport done, platformCode={}, submitted={}, accepted={}, deduplicated={}",
                    resultModel.getPlatformCode(),
                    resultModel.getSubmittedCount(),
                    resultModel.getAcceptedCount(),
                    resultModel.getDeduplicatedCount());
            return Result.ok(resultModel);
        } catch (AdminImportValidationException ex) {
            log.warn("AdminImportController.submitImport validation failed, code={}, message={}",
                    ex.getCode(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Result.fail(ex.getCode(), ex.getMessage());
        }
    }

    @PostMapping("/tasks/progress")
    public Result<AdminImportTaskProgressModel> queryImportTaskProgress(
            @RequestBody AdminImportTaskProgressQueryDTO requestDTO,
            HttpServletResponse response) {
        try {
            log.info("AdminImportController.queryImportTaskProgress start, taskCount={}",
                    requestDTO == null || requestDTO.getTaskIds() == null ? 0 : requestDTO.getTaskIds().size());
            AdminImportTaskProgressModel progressModel = adminContentImportService.queryTaskProgress(
                    requestDTO == null ? null : requestDTO.getTaskIds()
            );
            log.info("AdminImportController.queryImportTaskProgress done, total={}, pending={}, running={}, success={}, failed={}",
                    progressModel.getTotalCount(),
                    progressModel.getPendingCount(),
                    progressModel.getRunningCount(),
                    progressModel.getSuccessCount(),
                    progressModel.getFailedCount());
            return Result.ok(progressModel);
        } catch (AdminImportValidationException ex) {
            log.warn("AdminImportController.queryImportTaskProgress validation failed, code={}, message={}",
                    ex.getCode(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return Result.fail(ex.getCode(), ex.getMessage());
        }
    }
}
