package com.secondbrain.exception;

/**
 * 业务异常类.
 * <p>用于封装业务逻辑中的异常情况</p>
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 创建业务异常（默认错误码500）.
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 创建业务异常（指定错误码）.
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建业务异常（指定消息和原因）.
     *
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    /**
     * 获取错误码.
     *
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }
}
