package com.lin.webtemplate.service.application;

import com.lin.webtemplate.facade.dto.AutoScanTaskView;
import com.lin.webtemplate.facade.dto.ManualBackfillTaskView;
import com.lin.webtemplate.facade.model.TaskSource;
import com.lin.webtemplate.facade.model.TaskStatus;
import com.lin.webtemplate.infrastructure.crawler.ArticleContentExtractor;
import com.lin.webtemplate.infrastructure.crawler.ArticleHtmlFetcher;
import com.lin.webtemplate.infrastructure.crawler.ExtractedArticle;
import com.lin.webtemplate.infrastructure.crawler.PublicPageArticleUrlExtractor;
import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import com.lin.webtemplate.infrastructure.entity.CrawlTaskEntity;
import com.lin.webtemplate.infrastructure.entity.CrawlTaskLogEntity;
import com.lin.webtemplate.infrastructure.repository.ArticleRawRepository;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskLogRepository;
import com.lin.webtemplate.infrastructure.repository.CrawlTaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能：手动 URL 回填业务编排，负责幂等任务创建与采集执行。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Service
public class ManualBackfillApplicationService {

    private static final DateTimeFormatter OUTPUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CrawlTaskRepository crawlTaskRepository;

    private final CrawlTaskLogRepository crawlTaskLogRepository;

    private final ArticleRawRepository articleRawRepository;

    private final ArticleHtmlFetcher articleHtmlFetcher;

    private final ArticleContentExtractor articleContentExtractor;

    private final PublicPageArticleUrlExtractor publicPageArticleUrlExtractor;

    private final int defaultAutoScanMaxCount;

    public ManualBackfillApplicationService(CrawlTaskRepository crawlTaskRepository,
                                            CrawlTaskLogRepository crawlTaskLogRepository,
                                            ArticleRawRepository articleRawRepository,
                                            ArticleHtmlFetcher articleHtmlFetcher,
                                            ArticleContentExtractor articleContentExtractor,
                                            PublicPageArticleUrlExtractor publicPageArticleUrlExtractor,
                                            @Value("${crawl.auto-scan.max-scan-count:20}") int defaultAutoScanMaxCount) {
        this.crawlTaskRepository = crawlTaskRepository;
        this.crawlTaskLogRepository = crawlTaskLogRepository;
        this.articleRawRepository = articleRawRepository;
        this.articleHtmlFetcher = articleHtmlFetcher;
        this.articleContentExtractor = articleContentExtractor;
        this.publicPageArticleUrlExtractor = publicPageArticleUrlExtractor;
        this.defaultAutoScanMaxCount = defaultAutoScanMaxCount;
    }

    @Transactional
    public ManualBackfillTaskView submitManualBackfill(String sourceUrl) {
        return submitBackfill(sourceUrl, TaskSource.MANUAL);
    }

    @Transactional
    public AutoScanTaskView submitAutoScan(String listPageUrl, Integer maxScanCount) {
        validateUrl(listPageUrl);

        String listPageHtml = articleHtmlFetcher.fetch(listPageUrl);
        List<String> articleUrls = publicPageArticleUrlExtractor.extract(
                listPageHtml,
                listPageUrl,
                resolveMaxScanCount(maxScanCount)
        );

        int createdCount = 0;
        List<ManualBackfillTaskView> taskViews = new ArrayList<>();
        for (String articleUrl : articleUrls) {
            ManualBackfillTaskView taskView = submitBackfill(articleUrl, TaskSource.AUTO);
            if (!taskView.isIdempotentHit()) {
                createdCount++;
            }
            taskViews.add(taskView);
        }

        AutoScanTaskView view = new AutoScanTaskView();
        view.setListPageUrl(listPageUrl);
        view.setDiscoveredCount(articleUrls.size());
        view.setCreatedCount(createdCount);
        view.setDuplicateCount(articleUrls.size() - createdCount);
        view.setTasks(taskViews);
        return view;
    }

    @Transactional(readOnly = true)
    public ManualBackfillTaskView queryLatestResult(String sourceUrl) {
        CrawlTaskEntity entity = sourceUrl == null || sourceUrl.isBlank()
                ? crawlTaskRepository.findLatestTask()
                : crawlTaskRepository.findLatestBySourceUrl(sourceUrl);
        if (entity == null) {
            return null;
        }
        return toView(entity, false);
    }

    private ManualBackfillTaskView submitBackfill(String sourceUrl, TaskSource taskSource) {
        validateUrl(sourceUrl);
        CrawlTaskEntity existingTask = crawlTaskRepository.findBySourceUrl(sourceUrl);
        if (existingTask != null) {
            return toView(existingTask, true);
        }

        CrawlTaskEntity newTask = createPendingTask(sourceUrl, taskSource);
        runTask(newTask);
        CrawlTaskEntity finishedTask = crawlTaskRepository.findBySourceUrl(sourceUrl);
        return toView(finishedTask, false);
    }

    private CrawlTaskEntity createPendingTask(String sourceUrl, TaskSource taskSource) {
        CrawlTaskEntity taskEntity = new CrawlTaskEntity();
        taskEntity.setSourceUrl(sourceUrl);
        taskEntity.setTaskSource(taskSource.name());
        taskEntity.setStatus(TaskStatus.PENDING.name());
        crawlTaskRepository.insert(taskEntity);
        appendLog(taskEntity.getId(), TaskStatus.PENDING.name(), null, null, taskSource.name() + "任务已创建");
        return taskEntity;
    }

    private int resolveMaxScanCount(Integer maxScanCount) {
        if (maxScanCount == null) {
            return defaultAutoScanMaxCount;
        }
        if (maxScanCount <= 0) {
            throw new IllegalArgumentException("maxScanCount 必须大于 0");
        }
        return maxScanCount;
    }

    private void runTask(CrawlTaskEntity taskEntity) {
        updateTaskStatus(taskEntity, TaskStatus.RUNNING, null, null);
        appendLog(taskEntity.getId(), TaskStatus.RUNNING.name(), null, null, "开始抓取文章");

        try {
            String html = articleHtmlFetcher.fetch(taskEntity.getSourceUrl());
            ExtractedArticle extractedArticle = articleContentExtractor.extract(html);

            ArticleRawEntity rawEntity = new ArticleRawEntity();
            rawEntity.setSourceUrl(taskEntity.getSourceUrl());
            rawEntity.setTitle(extractedArticle.getTitle());
            rawEntity.setPublishTime(extractedArticle.getPublishTime());
            rawEntity.setHtmlContent(html);
            rawEntity.setContentText(extractedArticle.getContentText());
            rawEntity.setFetchedAt(LocalDateTime.now());
            articleRawRepository.upsert(rawEntity);
            ArticleRawEntity persisted = articleRawRepository.findBySourceUrl(taskEntity.getSourceUrl());

            taskEntity.setArticleId(persisted == null ? null : persisted.getId());
            updateTaskStatus(taskEntity, TaskStatus.SUCCESS, null, null);
            appendLog(taskEntity.getId(), TaskStatus.SUCCESS.name(), null, null, "抓取并落库成功");
        } catch (Exception exception) {
            // 采集执行错误统一落为失败，保留机器可读与可读错误信息。
            updateTaskStatus(taskEntity, TaskStatus.FAILED, "MANUAL_BACKFILL_FAILED", exception.getMessage());
            appendLog(taskEntity.getId(), TaskStatus.FAILED.name(), "MANUAL_BACKFILL_FAILED", exception.getMessage(), "抓取失败");
        }
    }

    private void updateTaskStatus(CrawlTaskEntity taskEntity, TaskStatus status, String errorCode, String errorMessage) {
        taskEntity.setStatus(status.name());
        taskEntity.setErrorCode(errorCode);
        taskEntity.setErrorMessage(errorMessage);
        taskEntity.setLastRunAt(LocalDateTime.now());
        crawlTaskRepository.updateStatusAndResult(taskEntity);
    }

    private void appendLog(Long taskId, String status, String errorCode, String errorMessage, String detail) {
        CrawlTaskLogEntity logEntity = new CrawlTaskLogEntity();
        logEntity.setTaskId(taskId);
        logEntity.setStatus(status);
        logEntity.setErrorCode(errorCode);
        logEntity.setErrorMessage(errorMessage);
        logEntity.setDetail(detail);
        crawlTaskLogRepository.insert(logEntity);
    }

    private ManualBackfillTaskView toView(CrawlTaskEntity taskEntity, boolean idempotentHit) {
        ManualBackfillTaskView view = new ManualBackfillTaskView();
        view.setTaskId(taskEntity.getId());
        view.setSourceUrl(taskEntity.getSourceUrl());
        view.setStatus(taskEntity.getStatus());
        view.setErrorCode(taskEntity.getErrorCode());
        view.setErrorMessage(taskEntity.getErrorMessage());
        view.setIdempotentHit(idempotentHit);

        ArticleRawEntity articleRawEntity = articleRawRepository.findBySourceUrl(taskEntity.getSourceUrl());
        if (articleRawEntity != null) {
            view.setTitle(articleRawEntity.getTitle());
            view.setContentText(articleRawEntity.getContentText());
            view.setPublishTime(articleRawEntity.getPublishTime() == null
                    ? null
                    : articleRawEntity.getPublishTime().format(OUTPUT_TIME));
        }
        return view;
    }

    private void validateUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl 不能为空");
        }
        try {
            URI uri = new URI(sourceUrl);
            if (uri.getScheme() == null || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("仅支持 http/https URL");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("URL 缺少 host");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL 格式不合法", exception);
        }
    }
}
