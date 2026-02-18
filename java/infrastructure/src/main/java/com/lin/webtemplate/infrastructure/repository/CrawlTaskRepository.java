package com.lin.webtemplate.infrastructure.repository;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.mapper.CrawlTaskMapper;

/**
 * 功能：采集任务仓储，负责任务创建、幂等检查与状态更新。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class CrawlTaskRepository {

    @Resource
    private CrawlTaskMapper crawlTaskMapper;

}
