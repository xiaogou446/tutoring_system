package com.lin.webtemplate.infrastructure.mapper;

import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 功能：文章原文 Mapper，负责原文记录查询与 upsert。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Mapper
public interface ArticleRawMapper {

    int upsert(ArticleRawEntity articleRawEntity);

    ArticleRawEntity findBySourceUrl(@Param("sourceUrl") String sourceUrl);

    ArticleRawEntity findById(@Param("id") Long id);

    List<ArticleRawEntity> findAll();
}
