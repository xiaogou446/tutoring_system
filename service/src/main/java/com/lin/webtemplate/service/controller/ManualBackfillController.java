package com.lin.webtemplate.service.controller;

import com.lin.webtemplate.facade.dto.AutoScanRequest;
import com.lin.webtemplate.facade.dto.AutoScanTaskView;
import com.lin.webtemplate.facade.dto.ManualBackfillRequest;
import com.lin.webtemplate.facade.dto.ManualBackfillTaskView;
import com.lin.webtemplate.facade.dto.Result;
import com.lin.webtemplate.service.application.ManualBackfillApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 功能：手动回填 API，提供触发与最近结果查询能力。
 *
 * @author linyi
 * @since 2026-02-16
 */
@RestController
@RequestMapping("/api/backfill")
public class ManualBackfillController {

    private final ManualBackfillApplicationService manualBackfillApplicationService;

    public ManualBackfillController(ManualBackfillApplicationService manualBackfillApplicationService) {
        this.manualBackfillApplicationService = manualBackfillApplicationService;
    }

    @PostMapping("/manual")
    public Result<ManualBackfillTaskView> submit(@Valid @RequestBody ManualBackfillRequest request) {
        try {
            return Result.ok(manualBackfillApplicationService.submitManualBackfill(request.getSourceUrl()));
        } catch (IllegalArgumentException exception) {
            return Result.fail("INVALID_URL", exception.getMessage());
        }
    }

    @PostMapping("/auto/scan")
    public Result<AutoScanTaskView> autoScan(@Valid @RequestBody AutoScanRequest request) {
        try {
            return Result.ok(manualBackfillApplicationService.submitAutoScan(request.getListPageUrl(), request.getMaxScanCount()));
        } catch (IllegalArgumentException exception) {
            return Result.fail("INVALID_URL", exception.getMessage());
        }
    }

    @GetMapping("/tasks/latest")
    public Result<ManualBackfillTaskView> latest(@RequestParam(value = "sourceUrl", required = false) String sourceUrl) {
        ManualBackfillTaskView latestTask = manualBackfillApplicationService.queryLatestResult(sourceUrl);
        if (latestTask == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到任务记录");
        }
        return Result.ok(latestTask);
    }
}
