package com.fanhua.jobtrack.security;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户主体，存放在 SecurityContext 中。
 * 不包含密码等敏感字段。
 */
@Getter
public class LoginUser implements Serializable {

    private final Long userId;
    private final String username;
    private final String sessionId;
    /** 角色列表，形如 ROLE_USER / ROLE_ADMIN */
    private final List<String> roles;

    public LoginUser(Long userId, String username, String sessionId, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.roles = roles;
    }
}
