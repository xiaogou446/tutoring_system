package com.lin.webtemplate.infrastructure.repository;

import com.lin.webtemplate.infrastructure.entity.CrawlTaskLogEntity;
import com.lin.webtemplate.infrastructure.mapper.CrawlTaskLogMapper;
import org.springframework.stereotype.Repository;

/**
 * 功能：任务日志仓储，统一记录任务轨迹。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class CrawlTaskLogRepository {

    private final CrawlTaskLogMapper crawlTaskLogMapper;

    public CrawlTaskLogRepository(CrawlTaskLogMapper crawlTaskLogMapper) {
        this.crawlTaskLogMapper = crawlTaskLogMapper;
    }

    public void insert(CrawlTaskLogEntity entity) {
        crawlTaskLogMapper.insert(entity);
    }
}
