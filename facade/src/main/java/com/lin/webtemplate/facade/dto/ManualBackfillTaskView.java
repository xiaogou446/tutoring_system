package com.lin.webtemplate.facade.dto;

/**
 * 功能：回填任务查询视图，返回任务状态与文章摘要信息。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class ManualBackfillTaskView {

    private Long taskId;

    private String sourceUrl;

    private String status;

    private String errorCode;

    private String errorMessage;

    private String title;

    private String publishTime;

    private String contentText;

    private boolean idempotentHit;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public boolean isIdempotentHit() {
        return idempotentHit;
    }

    public void setIdempotentHit(boolean idempotentHit) {
        this.idempotentHit = idempotentHit;
    }
}
