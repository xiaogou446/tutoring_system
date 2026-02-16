package com.lin.webtemplate.infrastructure.mapper;

import com.lin.webtemplate.infrastructure.entity.CrawlTaskLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 功能：任务日志 Mapper，记录状态变更轨迹。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface CrawlTaskLogMapper {

    int insert(CrawlTaskLogEntity crawlTaskLogEntity);
}
