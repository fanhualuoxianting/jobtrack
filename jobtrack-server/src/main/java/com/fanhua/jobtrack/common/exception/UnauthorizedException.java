package com.fanhua.jobtrack.common.exception;

/**
 * 未认证异常 (401)：Token 缺失、无效、过期或会话被注销
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
