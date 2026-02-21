package com.lin.webtemplate.infrastructure.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lin.webtemplate.infrastructure.dataobject.CrawlPlatformDO;

/**
 * 功能：采集平台主数据表的 MyBatis Mapper（XML 定义 SQL）。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Mapper
public interface CrawlPlatformMapper {

    List<CrawlPlatformDO> selectByStatus(@Param("status") String status);

    CrawlPlatformDO selectByPlatformCode(@Param("platformCode") String platformCode);
}
