package com.lin.webtemplate.infrastructure.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 功能：从文章 HTML 中提取基础字段并提供文本兜底。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Component
public class ArticleContentExtractor {

    private static final DateTimeFormatter SIMPLE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExtractedArticle extract(String html) {
        Document document = Jsoup.parse(html);
        ExtractedArticle extractedArticle = new ExtractedArticle();
        extractedArticle.setTitle(document.title());
        extractedArticle.setPublishTime(extractPublishTime(document));
        extractedArticle.setContentText(extractContentText(document));
        return extractedArticle;
    }

    private LocalDateTime extractPublishTime(Document document) {
        Element articleTime = document.selectFirst("meta[property=article:published_time]");
        if (articleTime != null) {
            LocalDateTime parsed = parseDateTime(articleTime.attr("content"));
            if (parsed != null) {
                return parsed;
            }
        }
        Element publishTime = document.selectFirst("meta[name=publish_time]");
        if (publishTime != null) {
            LocalDateTime parsed = parseDateTime(publishTime.attr("content"));
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private String extractContentText(Document document) {
        Element article = document.selectFirst("article");
        if (article != null && !article.text().isBlank()) {
            return article.text();
        }
        return document.body() == null ? "" : document.body().text();
    }

    private LocalDateTime parseDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(rawValue).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 优先兼容带时区格式，失败后再回退到简单格式。
        }
        try {
            return LocalDateTime.parse(rawValue, SIMPLE_TIME);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
