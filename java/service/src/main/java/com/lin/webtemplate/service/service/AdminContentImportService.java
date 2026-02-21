package com.lin.webtemplate.service.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.lin.webtemplate.infrastructure.dataobject.CrawlPlatformDO;
import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskDO;
import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskLogDO;
import com.lin.webtemplate.infrastructure.repository.CrawlPlatformRepository;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskLogRepository;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskRepository;
import com.lin.webtemplate.service.model.AdminImportAcceptResultModel;
import com.lin.webtemplate.service.model.AdminImportTaskProgressItemModel;
import com.lin.webtemplate.service.model.AdminImportTaskProgressModel;
import com.lin.webtemplate.service.model.AdminPlatformOptionModel;

/**
 * 功能：后台批量导入编排服务，负责平台查询、参数校验与任务受理。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Slf4j
@Service
public class AdminContentImportService {

    private static final int MAX_IMPORT_URLS = 10;

    @Resource
    private CrawlPlatformRepository crawlPlatformRepository;

    @Resource
    private PythonCrawlerCommandService pythonCrawlerCommandService;

    @Resource
    private CrawlTaskRepository crawlTaskRepository;

    @Resource
    private CrawlTaskLogRepository crawlTaskLogRepository;

    private ExecutorService importExecutor;

    @PostConstruct
    public void init() {
        this.importExecutor = Executors.newFixedThreadPool(2);
    }

    public List<AdminPlatformOptionModel> listEnabledPlatformOptions() {
        log.info("AdminContentImportService.listEnabledPlatformOptions start");
        List<CrawlPlatformDO> platformList = crawlPlatformRepository.findEnabledPlatforms();
        List<AdminPlatformOptionModel> options = new ArrayList<>(platformList.size());
        for (CrawlPlatformDO platformDO : platformList) {
            AdminPlatformOptionModel optionModel = new AdminPlatformOptionModel();
            optionModel.setPlatformCode(platformDO.getPlatformCode());
            optionModel.setPlatformName(platformDO.getPlatformName());
            options.add(optionModel);
        }
        log.info("AdminContentImportService.listEnabledPlatformOptions done, optionCount={}", options.size());
        return options;
    }

    public AdminImportAcceptResultModel acceptImport(String platformCode,
                                                     List<String> urls) {
        log.info("AdminContentImportService.acceptImport start, platformCode={}, urlCount={}",
                platformCode, urls == null ? 0 : urls.size());
        String normalizedPlatformCode = normalizePlatformCode(platformCode);
        List<String> normalizedUrls = normalizeAndValidateUrls(urls);
        validatePlatformCode(normalizedPlatformCode);

        Set<String> uniqueUrls = new LinkedHashSet<>(normalizedUrls);
        List<Long> taskIds = new ArrayList<>(uniqueUrls.size());
        for (String url : uniqueUrls) {
            Long taskId = pythonCrawlerCommandService.createOrReuseTask(url, normalizedPlatformCode, "MANUAL");
            taskIds.add(taskId);
            CrawlTaskDO taskDO = crawlTaskRepository.findById(taskId);
            if (taskDO != null && !shouldTriggerExecution(taskDO.getStatus())) {
                log.info("Skip import execution for task {}, status={}", taskId, taskDO.getStatus());
                continue;
            }
            importExecutor.submit(() -> {
                try {
                    pythonCrawlerCommandService.executeImportTask(taskId, false);
                } catch (Exception ex) {
                    log.warn("Admin import execute failed, taskId={}, platformCode={}, url={}",
                            taskId, normalizedPlatformCode, url, ex);
                }
            });
        }

        AdminImportAcceptResultModel resultModel = new AdminImportAcceptResultModel();
        resultModel.setPlatformCode(normalizedPlatformCode);
        resultModel.setSubmittedCount(normalizedUrls.size());
        resultModel.setAcceptedCount(uniqueUrls.size());
        resultModel.setDeduplicatedCount(normalizedUrls.size() - uniqueUrls.size());
        resultModel.setTaskIds(taskIds);
        log.info("AdminContentImportService.acceptImport done, platformCode={}, submitted={}, accepted={}, deduplicated={}",
                normalizedPlatformCode,
                normalizedUrls.size(),
                uniqueUrls.size(),
                normalizedUrls.size() - uniqueUrls.size());
        return resultModel;
    }

    public AdminImportTaskProgressModel queryTaskProgress(List<Long> taskIds) {
        log.info("AdminContentImportService.queryTaskProgress start, taskCount={}", taskIds == null ? 0 : taskIds.size());
        if (taskIds == null || taskIds.isEmpty()) {
            throw new AdminImportValidationException("TASK_IDS_REQUIRED", "任务ID列表不能为空");
        }
        if (taskIds.size() > 50) {
            throw new AdminImportValidationException("TASK_QUERY_LIMIT_EXCEEDED", "单次最多查询 50 个任务");
        }

        List<CrawlTaskDO> taskList = crawlTaskRepository.findByIds(taskIds);
        List<CrawlTaskLogDO> latestLogs = crawlTaskLogRepository.findLatestByTaskIds(taskIds);
        Map<Long, CrawlTaskDO> taskMap = new HashMap<>(taskList.size());
        for (CrawlTaskDO taskDO : taskList) {
            taskMap.put(taskDO.getId(), taskDO);
        }
        Map<Long, CrawlTaskLogDO> latestLogMap = new HashMap<>(latestLogs.size());
        for (CrawlTaskLogDO taskLogDO : latestLogs) {
            latestLogMap.put(taskLogDO.getTaskId(), taskLogDO);
        }

        List<AdminImportTaskProgressItemModel> items = new ArrayList<>(taskIds.size());
        int pendingCount = 0;
        int runningCount = 0;
        int successCount = 0;
        int failedCount = 0;
        for (Long taskId : taskIds) {
            CrawlTaskDO taskDO = taskMap.get(taskId);
            AdminImportTaskProgressItemModel item = new AdminImportTaskProgressItemModel();
            item.setTaskId(taskId);

            if (taskDO == null) {
                item.setStatus("NOT_FOUND");
                item.setFinished(true);
                item.setLatestErrorType("TASK_NOT_FOUND");
                item.setLatestErrorMessage("任务不存在，可能已被清理");
                failedCount++;
                items.add(item);
                continue;
            }

            item.setSourceUrl(taskDO.getSourceUrl());
            item.setStatus(taskDO.getStatus());
            item.setFinished(isTerminalStatus(taskDO.getStatus()));

            CrawlTaskLogDO latestLog = latestLogMap.get(taskId);
            if (latestLog != null) {
                item.setLatestStage(latestLog.getStage());
                item.setLatestRuntime(latestLog.getRuntime());
                item.setLatestErrorType(latestLog.getErrorType());
                item.setLatestErrorSummary(latestLog.getErrorSummary());
                item.setLatestErrorMessage(latestLog.getErrorMessage());
            }

            switch (taskDO.getStatus()) {
                case "PENDING" -> pendingCount++;
                case "RUNNING" -> runningCount++;
                case "SUCCESS" -> successCount++;
                case "FAILED" -> failedCount++;
                default -> {
                    // 未知状态按失败计入，避免前端将异常状态当作成功。
                    failedCount++;
                }
            }
            items.add(item);
        }

        AdminImportTaskProgressModel progressModel = new AdminImportTaskProgressModel();
        progressModel.setTotalCount(taskIds.size());
        progressModel.setPendingCount(pendingCount);
        progressModel.setRunningCount(runningCount);
        progressModel.setSuccessCount(successCount);
        progressModel.setFailedCount(failedCount);
        progressModel.setFinishedCount(successCount + failedCount);
        progressModel.setAllFinished(progressModel.getFinishedCount() == taskIds.size());
        progressModel.setItems(items);
        log.info("AdminContentImportService.queryTaskProgress done, total={}, pending={}, running={}, success={}, failed={}",
                progressModel.getTotalCount(),
                progressModel.getPendingCount(),
                progressModel.getRunningCount(),
                progressModel.getSuccessCount(),
                progressModel.getFailedCount());
        return progressModel;
    }

    private static boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status) || "NOT_FOUND".equals(status);
    }

    private static boolean shouldTriggerExecution(String status) {
        return !"RUNNING".equals(status) && !"SUCCESS".equals(status);
    }

    private static String normalizePlatformCode(String platformCode) {
        String normalized = platformCode == null ? "" : platformCode.trim();
        if (normalized.isEmpty()) {
            throw new AdminImportValidationException("PLATFORM_REQUIRED", "平台编码不能为空");
        }
        return normalized;
    }

    private static List<String> normalizeAndValidateUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new AdminImportValidationException("URLS_REQUIRED", "URL 列表不能为空");
        }
        if (urls.size() > MAX_IMPORT_URLS) {
            throw new AdminImportValidationException("IMPORT_LIMIT_EXCEEDED", "单次最多导入 10 条");
        }

        List<String> normalizedUrls = new ArrayList<>(urls.size());
        for (String url : urls) {
            String normalized = url == null ? "" : url.trim();
            if (normalized.isEmpty()) {
                throw new AdminImportValidationException("URL_INVALID", "URL 不能为空");
            }
            validateHttpUrl(normalized);
            normalizedUrls.add(normalized);
        }
        return normalizedUrls;
    }

    private void validatePlatformCode(String platformCode) {
        CrawlPlatformDO platformDO = crawlPlatformRepository.findByPlatformCode(platformCode);
        if (platformDO == null) {
            throw new AdminImportValidationException("PLATFORM_NOT_FOUND", "平台编码不存在");
        }
        if (!"ENABLED".equals(platformDO.getStatus())) {
            throw new AdminImportValidationException("PLATFORM_DISABLED", "平台未启用，不可导入");
        }
    }

    private static void validateHttpUrl(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if ((scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)))
                || uri.getHost() == null
                || uri.getHost().isBlank()) {
            throw new AdminImportValidationException("URL_INVALID", "URL 非法: " + url);
        }
    }
}
