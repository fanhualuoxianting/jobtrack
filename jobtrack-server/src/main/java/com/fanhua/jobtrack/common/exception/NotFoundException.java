package com.fanhua.jobtrack.common.exception;

/**
 * 资源不存在异常 (404)
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public NotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", resource + " 不存在: " + id);
    }

    /** 需要专用错误码的 404 场景（如简历记录存在但物理文件缺失） */
    public NotFoundException(String errorCode, String message, boolean withCustomCode) {
        super(errorCode, message);
    }
}
