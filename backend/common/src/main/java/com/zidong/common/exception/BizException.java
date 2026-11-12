package com.zidong.common.exception;

import org.springframework.http.HttpStatus;

/** 业务异常：携带期望的 HTTP 状态，交由全局异常处理器还原为对应的响应码与提示 */
public class BizException extends RuntimeException {

    /** 未显式指定状态时的兜底：按服务器内部错误处理 */
    private static final HttpStatus DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    /** 期望返回给调用方的 HTTP 状态 */
    private final HttpStatus status;

    public BizException(String message) {
        this(DEFAULT_STATUS, message);
    }

    public BizException(HttpStatus status, String message) {
        super(message);
        this.status = status == null ? DEFAULT_STATUS : status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BizException badRequest(String message) {
        return new BizException(HttpStatus.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(HttpStatus.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(HttpStatus.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(HttpStatus.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(HttpStatus.CONFLICT, message);
    }
}
