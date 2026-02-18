package com.lin.webtemplate.infrastructure.repository;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

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

}
