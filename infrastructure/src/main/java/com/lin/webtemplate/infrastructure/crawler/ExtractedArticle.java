package com.lin.webtemplate.infrastructure.crawler;

import java.time.LocalDateTime;

/**
 * 功能：文章解析结果对象，封装标题、发布时间与文本内容。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class ExtractedArticle {

    private String title;

    private LocalDateTime publishTime;

    private String contentText;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }
}
