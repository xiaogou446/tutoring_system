package com.lin.webtemplate.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskLogDO;
import com.lin.webtemplate.infrastructure.mapper.CrawlTaskLogMapper;

/**
 * 功能：任务执行日志仓储，提供时间线与失败原因的持久化接口。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class CrawlTaskLogRepository {

    @Resource
    private CrawlTaskLogMapper crawlTaskLogMapper;

    public void insert(CrawlTaskLogDO taskLog) {
        crawlTaskLogMapper.insert(taskLog);
    }

    public List<CrawlTaskLogDO> findLatestByTaskIds(List<Long> taskIds) {
        return crawlTaskLogMapper.selectLatestByTaskIds(taskIds);
    }

    public List<CrawlTaskLogDO> findForRuntimeLog(String keyword,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  Integer limit) {
        return crawlTaskLogMapper.selectForRuntimeLog(keyword, startTime, endTime, limit);
    }

}
