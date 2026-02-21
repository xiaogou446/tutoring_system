package com.lin.webtemplate.service.dto;

import lombok.Data;

/**
 * 功能：后台管理员登录请求参数。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Data
public class AdminLoginRequestDTO {

    private String username;

    private String password;
}
