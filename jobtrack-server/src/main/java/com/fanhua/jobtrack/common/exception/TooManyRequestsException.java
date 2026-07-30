package com.fanhua.jobtrack.common.exception;

/**
 * 请求限流异常 (429)
 */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
