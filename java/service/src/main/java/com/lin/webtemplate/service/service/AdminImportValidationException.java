package com.lin.webtemplate.service.service;

/**
 * 功能：后台导入参数校验异常，承载标准化错误码。
 *
 * @author linyi
 * @since 2026-02-19
 */
public class AdminImportValidationException extends RuntimeException {

    private final String code;

    public AdminImportValidationException(String code,
                                          String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
