package com.lin.webtemplate.infrastructure.repository;

import com.lin.webtemplate.infrastructure.entity.ArticleRawEntity;
import com.lin.webtemplate.infrastructure.mapper.ArticleRawMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 功能：文章原文仓储，封装 Mapper 访问。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Repository
public class ArticleRawRepository {

    private final ArticleRawMapper articleRawMapper;

    public ArticleRawRepository(ArticleRawMapper articleRawMapper) {
        this.articleRawMapper = articleRawMapper;
    }

    public void upsert(ArticleRawEntity entity) {
        articleRawMapper.upsert(entity);
    }

    public ArticleRawEntity findBySourceUrl(String sourceUrl) {
        return articleRawMapper.findBySourceUrl(sourceUrl);
    }

    public ArticleRawEntity findById(Long id) {
        return articleRawMapper.findById(id);
    }

    public List<ArticleRawEntity> findAll() {
        return articleRawMapper.findAll();
    }
}
