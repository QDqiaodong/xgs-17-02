package com.xgs.water.exception;

/**
 * 业务异常：携带HTTP语义状态码
 * 409 专门用于并发冲突/重复提交，前端据此提示“已被他人处理”并刷新
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
