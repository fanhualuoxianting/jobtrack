package com.fanhua.jobtrack.common.exception;

/**
 * 登录锁定异常 (423)：短时间连续失败导致的临时锁定
 */
public class LockedException extends BusinessException {

    public LockedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
