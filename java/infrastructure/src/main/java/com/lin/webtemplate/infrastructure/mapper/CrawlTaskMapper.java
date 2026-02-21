package com.lin.webtemplate.infrastructure.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskDO;

/**
 * 功能：采集任务主表的 MyBatis Mapper（XML 定义 SQL）。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface CrawlTaskMapper {

    int insert(CrawlTaskDO crawlTask);

    CrawlTaskDO selectById(@Param("id") Long id);

    CrawlTaskDO selectBySourceUrlAndPlatform(@Param("sourceUrl") String sourceUrl,
                                             @Param("platformCode") String platformCode);

    List<CrawlTaskDO> selectByIds(@Param("ids") List<Long> ids);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status);
}
