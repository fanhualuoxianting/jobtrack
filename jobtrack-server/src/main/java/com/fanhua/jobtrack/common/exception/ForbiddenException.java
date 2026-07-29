package com.fanhua.jobtrack.common.exception;

/**
 * 无权访问异常 (403)
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super("RESOURCE_FORBIDDEN", message);
    }
}
