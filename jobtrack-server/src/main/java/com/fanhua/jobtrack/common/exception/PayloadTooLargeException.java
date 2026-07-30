package com.fanhua.jobtrack.common.exception;

/**
 * 请求体过大异常 (413)：业务层文件大小检查与 Spring Multipart 层共用同一 HTTP 语义
 */
public class PayloadTooLargeException extends BusinessException {

    public PayloadTooLargeException(String errorCode, String message) {
        super(errorCode, message);
    }
}
