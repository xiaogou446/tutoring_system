package com.lin.webtemplate.service.model;

import lombok.Data;

/**
 * 功能：后台管理员登录返回模型。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminLoginResultModel {

    private Long userId;

    private String username;

    private long expireAtEpochSeconds;
}
