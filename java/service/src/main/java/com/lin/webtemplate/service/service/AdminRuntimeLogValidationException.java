package com.lin.webtemplate.service.service;

import lombok.Getter;

/**
 * 功能：后台日志查询参数校验异常。
 *
 * @author linyi
 * @since 2026-02-21
 */
@Getter
public class AdminRuntimeLogValidationException extends RuntimeException {

    private final String code;

    public AdminRuntimeLogValidationException(String code,
                                              String message) {
        super(message);
        this.code = code;
    }
}
