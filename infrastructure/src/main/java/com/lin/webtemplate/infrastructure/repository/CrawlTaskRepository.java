package com.lin.webtemplate.infrastructure.repository;

import com.lin.webtemplate.infrastructure.entity.CrawlTaskEntity;
import com.lin.webtemplate.infrastructure.mapper.CrawlTaskMapper;
import org.springframework.stereotype.Repository;

/**
 * 功能：任务仓储，封装任务主表的核心读写。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class CrawlTaskRepository {

    private final CrawlTaskMapper crawlTaskMapper;

    public CrawlTaskRepository(CrawlTaskMapper crawlTaskMapper) {
        this.crawlTaskMapper = crawlTaskMapper;
    }

    public CrawlTaskEntity findBySourceUrl(String sourceUrl) {
        return crawlTaskMapper.findBySourceUrl(sourceUrl);
    }

    public void insert(CrawlTaskEntity entity) {
        crawlTaskMapper.insert(entity);
    }

    public void updateStatusAndResult(CrawlTaskEntity entity) {
        crawlTaskMapper.updateStatusAndResult(entity);
    }

    public CrawlTaskEntity findLatestBySourceUrl(String sourceUrl) {
        return crawlTaskMapper.findLatestBySourceUrl(sourceUrl);
    }

    public CrawlTaskEntity findLatestTask() {
        return crawlTaskMapper.findLatestTask();
    }
}
