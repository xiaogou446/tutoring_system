package com.lin.webtemplate.service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskLogDO;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskLogRepository;
import com.lin.webtemplate.service.config.AdminRuntimeLogProperties;
import com.lin.webtemplate.service.dto.AdminRuntimeLogQueryDTO;
import com.lin.webtemplate.service.model.AdminRuntimeLogItemModel;
import com.lin.webtemplate.service.model.AdminRuntimeLogQueryResultModel;

/**
 * 功能：后台日志中心查询服务，聚合 Java 文件日志与 Python 任务日志。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Service
@Slf4j
public class AdminRuntimeLogService {

    private static final String RUNTIME_JAVA = "java";

    private static final String RUNTIME_PYTHON = "python";

    private static final Pattern JAVA_LOG_LINE_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(INFO|WARN|ERROR)\\s+.*? - (.*)$"
    );

    private static final DateTimeFormatter JAVA_LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Resource
    private AdminRuntimeLogProperties adminRuntimeLogProperties;

    @Resource
    private CrawlTaskLogRepository crawlTaskLogRepository;

    public AdminRuntimeLogQueryResultModel query(AdminRuntimeLogQueryDTO requestDTO) {
        if (requestDTO == null) {
            throw new AdminRuntimeLogValidationException("RUNTIME_REQUIRED", "runtime 不能为空");
        }
        log.info("AdminRuntimeLogService.query start, runtime={}, level={}, keyword={}, startTime={}, endTime={}, limit={}",
                requestDTO.getRuntime(), requestDTO.getLevel(), requestDTO.getKeyword(),
                requestDTO.getStartTime(), requestDTO.getEndTime(), requestDTO.getLimit());
        String runtime = normalizeRuntime(requestDTO.getRuntime());
        String level = normalizeLevel(requestDTO.getLevel());
        String keyword = normalizeKeyword(requestDTO.getKeyword());
        LocalDateTime startTime = requestDTO.getStartTime();
        LocalDateTime endTime = requestDTO.getEndTime();
        validateTimeRange(startTime, endTime);
        int limit = normalizeLimit(requestDTO.getLimit());

        List<AdminRuntimeLogItemModel> items = RUNTIME_JAVA.equals(runtime)
                ? queryJavaLogs(level, keyword, startTime, endTime, limit)
                : queryPythonLogs(level, keyword, startTime, endTime, limit);

        AdminRuntimeLogQueryResultModel resultModel = new AdminRuntimeLogQueryResultModel();
        resultModel.setRuntime(runtime);
        resultModel.setTotalCount(items.size());
        resultModel.setItems(items);
        log.info("AdminRuntimeLogService.query done, runtime={}, totalCount={}", runtime, items.size());
        return resultModel;
    }

    private List<AdminRuntimeLogItemModel> queryJavaLogs(String level,
                                                         String keyword,
                                                         LocalDateTime startTime,
                                                         LocalDateTime endTime,
                                                         int limit) {
        Path logDir = Path.of(adminRuntimeLogProperties.getJavaLogDir());
        if (!Files.exists(logDir)) {
            return List.of();
        }

        List<AdminRuntimeLogItemModel> matchedItems = new ArrayList<>();
        LocalDateTime retentionStart = LocalDateTime.now().minusDays(adminRuntimeLogProperties.getRetentionDays());

        try (Stream<Path> pathStream = Files.list(logDir)) {
            List<Path> logFiles = pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isJavaLogFile)
                    .toList();

            for (Path logFile : logFiles) {
                List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
                for (String line : lines) {
                    Matcher matcher = JAVA_LOG_LINE_PATTERN.matcher(line);
                    if (!matcher.matches()) {
                        continue;
                    }

                    LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), JAVA_LOG_TIME_FORMATTER);
                    if (timestamp.isBefore(retentionStart)) {
                        continue;
                    }

                    String itemLevel = matcher.group(2);
                    String message = matcher.group(3);
                    if (!matchesLevel(level, itemLevel)
                            || !matchesTimeRange(startTime, endTime, timestamp)
                            || !matchesKeyword(keyword, message)) {
                        continue;
                    }

                    AdminRuntimeLogItemModel item = new AdminRuntimeLogItemModel();
                    item.setRuntime(RUNTIME_JAVA);
                    item.setLevel(itemLevel);
                    item.setTimestamp(timestamp);
                    item.setMessage(message);
                    matchedItems.add(item);
                }
            }
        } catch (IOException ex) {
            log.warn("AdminRuntimeLogService.queryJavaLogs read failed, dir={}", logDir, ex);
            throw new AdminRuntimeLogValidationException("JAVA_LOG_READ_ERROR", "Java 日志读取失败");
        }

        return matchedItems.stream()
                .sorted(Comparator.comparing(AdminRuntimeLogItemModel::getTimestamp).reversed())
                .limit(limit)
                .toList();
    }

    private List<AdminRuntimeLogItemModel> queryPythonLogs(String level,
                                                           String keyword,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime,
                                                           int limit) {
        List<CrawlTaskLogDO> taskLogs = crawlTaskLogRepository.findForRuntimeLog(keyword, startTime, endTime, limit);
        List<AdminRuntimeLogItemModel> items = new ArrayList<>(taskLogs.size());
        for (CrawlTaskLogDO taskLog : taskLogs) {
            String runtime = normalizeTaskLogRuntime(taskLog.getRuntime());
            if (!RUNTIME_PYTHON.equals(runtime)) {
                continue;
            }
            String mappedLevel = mapPythonLevel(taskLog.getStatus());
            if (!matchesLevel(level, mappedLevel)) {
                continue;
            }

            AdminRuntimeLogItemModel item = new AdminRuntimeLogItemModel();
            item.setRuntime(runtime);
            item.setLevel(mappedLevel);
            item.setTimestamp(taskLog.getCreatedAt());
            item.setMessage(buildPythonMessage(taskLog));
            items.add(item);
        }
        return items;
    }

    private boolean isJavaLogFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(adminRuntimeLogProperties.getJavaFilePrefix()) && fileName.endsWith(".log");
    }

    private static String normalizeRuntime(String runtime) {
        String normalized = runtime == null ? "" : runtime.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new AdminRuntimeLogValidationException("RUNTIME_REQUIRED", "runtime 不能为空");
        }
        if (!RUNTIME_JAVA.equals(normalized) && !RUNTIME_PYTHON.equals(normalized)) {
            throw new AdminRuntimeLogValidationException("RUNTIME_INVALID", "runtime 仅支持 java 或 python");
        }
        return normalized;
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        String normalized = level.trim().toUpperCase(Locale.ROOT);
        if (!"INFO".equals(normalized) && !"WARN".equals(normalized) && !"ERROR".equals(normalized)) {
            throw new AdminRuntimeLogValidationException("LEVEL_INVALID", "level 仅支持 INFO/WARN/ERROR");
        }
        return normalized;
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return adminRuntimeLogProperties.getMaxQueryLines();
        }
        if (limit <= 0 || limit > adminRuntimeLogProperties.getMaxQueryLines()) {
            throw new AdminRuntimeLogValidationException(
                    "LIMIT_INVALID",
                    "limit 必须在 1 ~ " + adminRuntimeLogProperties.getMaxQueryLines() + " 之间"
            );
        }
        return limit;
    }

    private static void validateTimeRange(LocalDateTime startTime,
                                          LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new AdminRuntimeLogValidationException("TIME_RANGE_INVALID", "endTime 不能早于 startTime");
        }
    }

    private static boolean matchesLevel(String expectedLevel,
                                        String actualLevel) {
        return expectedLevel == null || expectedLevel.equals(actualLevel);
    }

    private static boolean matchesTimeRange(LocalDateTime startTime,
                                            LocalDateTime endTime,
                                            LocalDateTime currentTime) {
        if (startTime != null && currentTime.isBefore(startTime)) {
            return false;
        }
        return endTime == null || !currentTime.isAfter(endTime);
    }

    private static boolean matchesKeyword(String keyword,
                                          String message) {
        if (keyword == null) {
            return true;
        }
        return message != null && message.contains(keyword);
    }

    private static String mapPythonLevel(String status) {
        if ("FAILED".equals(status)) {
            return "ERROR";
        }
        if ("RUNNING".equals(status)) {
            return "WARN";
        }
        return "INFO";
    }

    private static String buildPythonMessage(CrawlTaskLogDO taskLog) {
        return "taskId=" + taskLog.getTaskId()
                + " runtime=" + normalizeTaskLogRuntime(taskLog.getRuntime())
                + " stage=" + taskLog.getStage()
                + " status=" + taskLog.getStatus()
                + " errorType=" + taskLog.getErrorType()
                + " errorSummary=" + taskLog.getErrorSummary()
                + " errorMessage=" + taskLog.getErrorMessage();
    }

    private static String normalizeTaskLogRuntime(String runtime) {
        if (runtime == null || runtime.isBlank()) {
            return RUNTIME_PYTHON;
        }
        return runtime.trim().toLowerCase(Locale.ROOT);
    }
}
