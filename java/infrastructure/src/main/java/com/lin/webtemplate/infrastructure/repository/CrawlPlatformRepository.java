package com.lin.webtemplate.infrastructure.repository;

import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.dataobject.CrawlPlatformDO;
import com.lin.webtemplate.infrastructure.mapper.CrawlPlatformMapper;

/**
 * 功能：采集平台主数据仓储，封装平台配置相关数据访问。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Repository
public class CrawlPlatformRepository {

    @Resource
    private CrawlPlatformMapper crawlPlatformMapper;

    public List<CrawlPlatformDO> findEnabledPlatforms() {
        return crawlPlatformMapper.selectByStatus("ENABLED");
    }

    public CrawlPlatformDO findByPlatformCode(String platformCode) {
        return crawlPlatformMapper.selectByPlatformCode(platformCode);
    }

}
