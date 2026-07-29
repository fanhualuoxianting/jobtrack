package com.fanhua.jobtrack.common.exception;

/**
 * 数据冲突异常 (409)，如乐观锁冲突、重复记录
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super("OPTIMISTIC_LOCK_CONFLICT", message);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
