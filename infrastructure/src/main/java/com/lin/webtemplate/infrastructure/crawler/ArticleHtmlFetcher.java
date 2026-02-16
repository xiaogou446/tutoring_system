package com.lin.webtemplate.infrastructure.crawler;

/**
 * 功能：文章抓取器抽象，统一按 URL 拉取原始 HTML。
 *
 * @author linyi
 * @since 2026-02-16
 */
public interface ArticleHtmlFetcher {

    String fetch(String sourceUrl);
}
