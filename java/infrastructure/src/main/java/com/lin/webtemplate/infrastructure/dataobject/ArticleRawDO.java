package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：公众号文章原文归档 DO，保存原始内容与关键时间字段。
 *
 * 该表为后续解析与回溯的事实来源；解析失败也应保留原文。
 * 通过 sourceUrl 做幂等，避免重复入库。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
public class ArticleRawDO {

    /** 主键ID。 */
    private Long id;

    /** 文章来源URL，作为幂等键。 */
    private String sourceUrl;

    /** 文章标题。 */
    private String title;

    /** 文章HTML正文。 */
    private String contentHtml;

    /** 文章纯文本正文。 */
    private String contentText;

    /** 文章发布时间。 */
    private LocalDateTime publishedAt;

    /** 抓取时间。 */
    private LocalDateTime crawledAt;
}
