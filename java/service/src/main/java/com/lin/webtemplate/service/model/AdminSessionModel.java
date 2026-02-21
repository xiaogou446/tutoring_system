package com.lin.webtemplate.service.model;

import lombok.Data;

/**
 * 功能：后台管理员认证会话模型。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminSessionModel {

    private Long userId;

    private String username;

    private long issuedAtEpochSeconds;

    private long expireAtEpochSeconds;

    private String token;
}
