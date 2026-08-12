package com.qiye.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static final int CODE_OK = 200;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_ERROR = 500;

    public static <T> Result<T> ok() {
        return build(CODE_OK, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return build(CODE_OK, "success", data);
    }

    public static <T> Result<T> error(String message) {
        return build(CODE_ERROR, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        return r;
    }
}
