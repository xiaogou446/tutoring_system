package com.lin.webtemplate.infrastructure.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lin.webtemplate.infrastructure.dataobject.CrawlTaskLogDO;

/**
 * 功能：任务执行日志表的 MyBatis Mapper（XML 定义 SQL）。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface CrawlTaskLogMapper {

    int insert(CrawlTaskLogDO taskLog);

    List<CrawlTaskLogDO> selectLatestByTaskIds(@Param("taskIds") List<Long> taskIds);
}
