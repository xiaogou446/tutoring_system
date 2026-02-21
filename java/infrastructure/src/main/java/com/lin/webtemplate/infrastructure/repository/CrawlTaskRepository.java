package com.lin.webtemplate.infrastructure.repository;

import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskDO;
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

    public Long insert(CrawlTaskDO crawlTask) {
        crawlTaskMapper.insert(crawlTask);
        return crawlTask.getId();
    }

    public CrawlTaskDO findById(Long id) {
        return crawlTaskMapper.selectById(id);
    }

    public CrawlTaskDO findBySourceUrlAndPlatform(String sourceUrl,
                                                  String platformCode) {
        return crawlTaskMapper.selectBySourceUrlAndPlatform(sourceUrl, platformCode);
    }

    public List<CrawlTaskDO> findByIds(List<Long> ids) {
        return crawlTaskMapper.selectByIds(ids);
    }

    public boolean updateStatus(Long id,
                                String status) {
        return crawlTaskMapper.updateStatus(id, status) > 0;
    }

}
