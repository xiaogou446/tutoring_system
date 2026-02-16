package com.lin.webtemplate.infrastructure.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 功能：从公众号公开列表页提取文章 URL，并做基础规范化与去重。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Component
public class PublicPageArticleUrlExtractor {

    public List<String> extract(String html, String baseUrl, int maxCount) {
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Document document = Jsoup.parse(html, baseUrl);
        Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select("a[href]")) {
            if (urls.size() >= maxCount) {
                break;
            }
            String absoluteUrl = normalizeUrl(link.attr("abs:href"));
            if (absoluteUrl == null || absoluteUrl.equals(baseUrl)) {
                continue;
            }
            if (absoluteUrl.startsWith("http://") || absoluteUrl.startsWith("https://")) {
                // LinkedHashSet 保证按页面出现顺序去重，便于任务回放与测试断言稳定。
                urls.add(absoluteUrl);
            }
        }
        return new ArrayList<>(urls);
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(rawUrl);
            URI normalized = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
            return normalized.toString();
        } catch (URISyntaxException exception) {
            return null;
        }
    }
}
