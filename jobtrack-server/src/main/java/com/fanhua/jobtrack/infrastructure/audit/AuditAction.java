package com.fanhua.jobtrack.infrastructure.audit;

/**
 * 审计动作常量，避免魔法字符串散落
 */
public final class AuditAction {

    private AuditAction() {
    }

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String LOGIN_LOCKED = "LOGIN_LOCKED";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String REFRESH_REPLAY_DETECTED = "REFRESH_REPLAY_DETECTED";
    public static final String LOGOUT = "LOGOUT";
    public static final String LOGOUT_ALL = "LOGOUT_ALL";
    public static final String SESSION_REVOKED = "SESSION_REVOKED";

    /** 资源类型 */
    public static final String RES_AUTH_SESSION = "AUTH_SESSION";
    public static final String RES_USER = "USER";
}
