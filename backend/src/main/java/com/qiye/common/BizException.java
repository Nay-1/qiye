package com.qiye.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        super(message);
        this.code = Result.CODE_ERROR;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
