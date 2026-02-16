package com.lin.webtemplate.infrastructure.mapper;

import com.lin.webtemplate.infrastructure.entity.CrawlTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 功能：任务主表 Mapper，提供任务的创建、更新与查询。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface CrawlTaskMapper {

    int insert(CrawlTaskEntity crawlTaskEntity);

    int updateStatusAndResult(CrawlTaskEntity crawlTaskEntity);

    CrawlTaskEntity findBySourceUrl(@Param("sourceUrl") String sourceUrl);

    CrawlTaskEntity findLatestBySourceUrl(@Param("sourceUrl") String sourceUrl);

    CrawlTaskEntity findLatestTask();
}
