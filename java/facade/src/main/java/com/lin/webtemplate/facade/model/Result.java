package com.lin.webtemplate.facade.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能：统一接口返回包装，避免在 Controller 层散落状态码与错误结构。
 *
 * 保持 HTTP 200 的前提下，通过 code/message 表达业务成功或失败。
 * data 为泛型负载，便于 H5/小程序统一消费。
 *
 * @author linyi
 * @since 2026-02-16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private boolean success;

    private String code;

    private String message;

    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "OK", "OK", data);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(false, code, message, null);
    }
}
