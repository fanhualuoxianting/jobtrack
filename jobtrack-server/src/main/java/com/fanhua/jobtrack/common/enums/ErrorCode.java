package com.fanhua.jobtrack.common.enums;

import lombok.Getter;

/**
 * 全局错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS("SUCCESS", "操作成功"),
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验失败"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "账号或密码错误"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "Access Token 过期"),
    AUTH_REFRESH_INVALID("AUTH_REFRESH_INVALID", "Refresh Token 无效"),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "账号被禁用"),
    USER_EMAIL_EXISTS("USER_EMAIL_EXISTS", "邮箱已存在"),
    USER_USERNAME_EXISTS("USER_USERNAME_EXISTS", "用户名已存在"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),
    RESOURCE_FORBIDDEN("RESOURCE_FORBIDDEN", "无权访问资源"),
    COMPANY_NAME_EXISTS("COMPANY_NAME_EXISTS", "公司名称重复"),
    COMPANY_HAS_RELATIONS("COMPANY_HAS_RELATIONS", "公司存在关联记录"),
    POSITION_HAS_APPLICATIONS("POSITION_HAS_APPLICATIONS", "岗位存在投递记录"),
    RESUME_FILE_TOO_LARGE("RESUME_FILE_TOO_LARGE", "简历文件过大"),
    RESUME_FILE_TYPE_UNSUPPORTED("RESUME_FILE_TYPE_UNSUPPORTED", "文件类型不支持"),
    RESUME_IN_USE("RESUME_IN_USE", "简历正在被投递记录使用"),
    APPLICATION_DUPLICATE("APPLICATION_DUPLICATE", "岗位已存在投递记录"),
    APPLICATION_INVALID_STATUS_TRANSITION("APPLICATION_INVALID_STATUS_TRANSITION", "非法状态流转"),
    INTERVIEW_TIME_INVALID("INTERVIEW_TIME_INVALID", "面试时间不合法"),
    REMINDER_TIME_INVALID("REMINDER_TIME_INVALID", "提醒时间不合法"),
    OPTIMISTIC_LOCK_CONFLICT("OPTIMISTIC_LOCK_CONFLICT", "数据已被其他请求修改"),
    RATE_LIMITED("RATE_LIMITED", "请求过于频繁"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
