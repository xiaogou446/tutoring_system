package com.lin.webtemplate.infrastructure.crawler;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 功能：基于 JDK HttpClient 的文章抓取实现。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Component
public class JdkArticleHtmlFetcher implements ArticleHtmlFetcher {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public JdkArticleHtmlFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    @Override
    public String fetch(String sourceUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 tutoring-system")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new IllegalStateException("HTTP 状态码异常: " + response.statusCode());
        } catch (IOException exception) {
            throw new IllegalStateException("抓取文章失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("抓取文章被中断", exception);
        }
    }
}
