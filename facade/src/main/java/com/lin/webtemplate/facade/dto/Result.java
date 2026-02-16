package com.lin.webtemplate.facade.dto;

/**
 * 功能：统一对外响应包装，承载成功标记、错误信息与数据体。
 *
 * @author linyi
 * @since 2026-02-16
 */
public class Result<T> {

    private boolean success;

    private String code;

    private String message;

    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.code = "OK";
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> Result<T> fail(String code, String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.code = code;
        result.message = message;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
