package com.lin.webtemplate.infrastructure.dataobject;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 功能：后台管理员账号 DO，承载登录鉴权所需的账号信息。
 *
 * 账号由数据库预置维护，
 * 仅支持登录，不包含注册流程。
 *
 * @author linyi
 * @since 2026-02-18
 */
@Data
public class AdminUserDO {

    /** 主键ID。 */
    private Long id;

    /** 登录用户名。 */
    private String username;

    /** 密码哈希。 */
    private String passwordHash;

    /** 账号状态。 */
    private String status;

    /** 最后登录时间。 */
    private LocalDateTime lastLoginAt;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
