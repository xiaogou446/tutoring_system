package com.lin.webtemplate.infrastructure.repository;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import com.lin.webtemplate.infrastructure.mapper.ArticleRawMapper;

/**
 * 功能：文章原文仓储，封装 Mapper 以保持 service 层与 SQL 解耦。
 *
 * MVP 阶段先提供最小骨架，后续按任务逐步补齐查询与写入方法。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class ArticleRawRepository {

    @Resource
    private ArticleRawMapper articleRawMapper;

}
