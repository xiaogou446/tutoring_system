package com.lin.webtemplate.service.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskDO;
import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskLogDO;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskLogRepository;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskRepository;
import com.lin.webtemplate.service.config.PythonCommandProperties;
import com.lin.webtemplate.service.model.PythonCommandExecutionResultModel;

/**
 * 功能：执行 Python 补采命令并回写任务日志。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Slf4j
@Service
public class PythonCrawlerCommandService {

    private static final int MAX_LOG_SNIPPET = 500;

    private static final String RUNTIME_JAVA = "java";

    private static final String RUNTIME_PYTHON = "python";

    @Resource
    private PythonCommandProperties pythonCommandProperties;

    @Resource
    private CrawlTaskRepository crawlTaskRepository;

    @Resource
    private CrawlTaskLogRepository crawlTaskLogRepository;

    private Semaphore commandSemaphore;

    @PostConstruct
    public void init() {
        int permits = Math.max(1, pythonCommandProperties.getMaxConcurrency());
        this.commandSemaphore = new Semaphore(permits);
    }

    public PythonCommandExecutionResultModel executeImportTask(Long taskId,
                                                               boolean fromArticleRaw) {
        CrawlTaskDO task = crawlTaskRepository.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("task not found: " + taskId);
        }
        if ("RUNNING".equals(task.getStatus()) || "SUCCESS".equals(task.getStatus())) {
            PythonCommandExecutionResultModel skipped = new PythonCommandExecutionResultModel();
            skipped.setSuccess(true);
            skipped.setExitCode(0);
            skipped.setStderr("skipped by task status=" + task.getStatus());
            return skipped;
        }

        List<String> command = buildImportCommand(task, fromArticleRaw);
        return executeCommand(taskId, command);
    }

    public Long createOrReuseTask(String sourceUrl,
                                  String platformCode,
                                  String sourceType) {
        CrawlTaskDO existing = crawlTaskRepository.findBySourceUrlAndPlatform(sourceUrl, platformCode);
        if (existing != null) {
            return existing.getId();
        }

        CrawlTaskDO task = new CrawlTaskDO();
        task.setSourceUrl(sourceUrl);
        task.setPlatformCode(platformCode);
        task.setSourceType(sourceType);
        task.setStatus("PENDING");
        return crawlTaskRepository.insert(task);
    }

    private PythonCommandExecutionResultModel executeCommand(Long taskId,
                                                             List<String> command) {
        PythonCommandExecutionResultModel result = new PythonCommandExecutionResultModel();
        ExecutorService streamPool = Executors.newFixedThreadPool(2);

        boolean acquired = false;
        Process process = null;
        try {
            acquired = commandSemaphore.tryAcquire(pythonCommandProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                markFailed(taskId, "JAVA_CONCURRENCY_LIMIT", "python command concurrency limit reached");
                result.setSuccess(false);
                result.setExitCode(-1);
                result.setStderr("python command concurrency limit reached");
                return result;
            }

            crawlTaskRepository.updateStatus(taskId, "RUNNING");
            appendLog(taskId, RUNTIME_JAVA, "JAVA_ORCHESTRATE", "RUNNING", "", "", "command started");
            appendLog(taskId, RUNTIME_PYTHON, "PYTHON_EXECUTE", "RUNNING", "", "", "command started");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new java.io.File(pythonCommandProperties.getWorkingDirectory()));
            process = processBuilder.start();
            final Process runningProcess = process;

            Future<String> stdoutFuture = streamPool.submit(() -> readStream(runningProcess.getInputStream()));
            Future<String> stderrFuture = streamPool.submit(() -> readStream(runningProcess.getErrorStream()));
            boolean finished = process.waitFor(pythonCommandProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                result.setTimeout(true);
                result.setSuccess(false);
                result.setExitCode(-1);
                process.destroyForcibly();
                result.setStdout(safeFuture(stdoutFuture));
                result.setStderr(safeFuture(stderrFuture));
                markFailed(taskId,
                        "PYTHON_TIMEOUT",
                        "timeout=" + pythonCommandProperties.getTimeoutSeconds() + "s; stderr="
                                + shorten(result.getStderr()) + "; stdout=" + shorten(result.getStdout()));
                return result;
            }

            int exitCode = process.exitValue();
            String stdout = safeFuture(stdoutFuture);
            String stderr = safeFuture(stderrFuture);
            result.setExitCode(exitCode);
            result.setStdout(stdout);
            result.setStderr(stderr);

            if (exitCode != 0) {
                result.setSuccess(false);
                markFailed(taskId,
                        "PYTHON_EXIT_NON_ZERO",
                        "exitCode=" + exitCode + "; stderr=" + shorten(stderr) + "; stdout=" + shorten(stdout));
                return result;
            }

            result.setSuccess(true);
            appendLog(taskId,
                    RUNTIME_JAVA,
                    "JAVA_ORCHESTRATE",
                    "SUCCESS",
                    "",
                    "exitCode=0",
                    "python command finished"
            );
            appendLog(taskId,
                    RUNTIME_PYTHON,
                    "PYTHON_EXECUTE",
                    "SUCCESS",
                    "",
                    "exitCode=0",
                    "exitCode=0; stdout=" + shorten(stdout) + "; stderr=" + shorten(stderr));
            return result;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            result.setSuccess(false);
            result.setExitCode(-1);
            markFailed(taskId, "JAVA_INTERRUPTED", ex.getMessage());
            return result;
        } catch (IOException ex) {
            result.setSuccess(false);
            result.setExitCode(-1);
            markFailed(taskId, "JAVA_IO_ERROR", ex.getMessage());
            return result;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            streamPool.shutdownNow();
            if (acquired) {
                commandSemaphore.release();
            }
        }
    }

    private List<String> buildImportCommand(CrawlTaskDO task,
                                            boolean fromArticleRaw) {
        List<String> command = new ArrayList<>();
        command.add(pythonCommandProperties.getPythonBin());
        command.add(pythonCommandProperties.getScriptPath());
        command.add("--db-type");
        command.add(pythonCommandProperties.getDbType());

        if (Objects.equals("sqlite", pythonCommandProperties.getDbType())) {
            command.add("--db");
            command.add(pythonCommandProperties.getSqlitePath());
        } else {
            command.add("--mysql-host");
            command.add(pythonCommandProperties.getMysqlHost());
            command.add("--mysql-port");
            command.add(String.valueOf(pythonCommandProperties.getMysqlPort()));
            command.add("--mysql-user");
            command.add(pythonCommandProperties.getMysqlUser());
            command.add("--mysql-password");
            command.add(pythonCommandProperties.getMysqlPassword());
            command.add("--mysql-database");
            command.add(pythonCommandProperties.getMysqlDatabase());
        }

        command.add("--platform-code");
        command.add(task.getPlatformCode());
        command.add("--source-type");
        command.add(task.getSourceType());
        command.add("--parser-mode");
        command.add(pythonCommandProperties.getParserMode());
        command.add("--llm-config");
        command.add(pythonCommandProperties.getLlmConfig());
        command.add("--url");
        command.add(task.getSourceUrl());
        if (fromArticleRaw) {
            command.add("--from-article-raw");
        }
        return command;
    }

    private void markFailed(Long taskId,
                            String errorType,
                            String errorMessage) {
        crawlTaskRepository.updateStatus(taskId, "FAILED");
        appendLog(taskId,
                RUNTIME_JAVA,
                "JAVA_ORCHESTRATE",
                "FAILED",
                errorType,
                summarizeError(errorMessage),
                shorten(errorMessage));
        appendLog(taskId,
                RUNTIME_PYTHON,
                "PYTHON_EXECUTE",
                "FAILED",
                errorType,
                summarizeError(errorMessage),
                shorten(errorMessage));
        log.warn("Python command failed, taskId={}, errorType={}, message={}",
                taskId, errorType, shorten(errorMessage));
    }

    private void appendLog(Long taskId,
                           String runtime,
                           String stage,
                           String status,
                           String errorType,
                           String errorSummary,
                           String errorMessage) {
        CrawlTaskLogDO taskLog = new CrawlTaskLogDO();
        LocalDateTime now = LocalDateTime.now();
        taskLog.setTaskId(taskId);
        taskLog.setRuntime(runtime == null ? RUNTIME_PYTHON : runtime);
        taskLog.setStage(stage);
        taskLog.setStatus(status);
        taskLog.setErrorType(errorType == null ? "" : errorType);
        taskLog.setErrorSummary(errorSummary == null ? "" : errorSummary);
        taskLog.setErrorMessage(errorMessage == null ? "" : errorMessage);
        taskLog.setStartedAt(now);
        taskLog.setFinishedAt(now);
        crawlTaskLogRepository.insert(taskLog);
    }

    private static String summarizeError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "";
        }
        String normalized = errorMessage.trim();
        int separatorIndex = normalized.indexOf(';');
        if (separatorIndex > 0) {
            return shorten(normalized.substring(0, separatorIndex));
        }
        return shorten(normalized);
    }

    private static String readStream(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String safeFuture(Future<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception ex) {
            return "";
        }
    }

    private static String shorten(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= MAX_LOG_SNIPPET) {
            return normalized;
        }
        return normalized.substring(0, MAX_LOG_SNIPPET) + "...";
    }
}
