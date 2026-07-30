package com.fanhua.jobtrack.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证安全策略配置：Refresh Cookie 与登录失败限流
 */
@Data
@Component
@ConfigurationProperties(prefix = "jobtrack.auth")
public class AuthProperties {

    /** Refresh Token Cookie 名称 */
    private String cookieName = "JOBTRACK_RT";

    /** HTTPS 环境下置 true，本地 HTTP 开发可保持 false */
    private boolean cookieSecure = false;

    /** 限制 Cookie 仅发送到认证相关路径，降低暴露面 */
    private String cookiePath = "/api/v1/auth";

    /** 窗口期内允许的最大连续失败次数 */
    private int loginFailMax = 5;

    /** 登录失败统计窗口（分钟） */
    private int loginFailWindowMinutes = 10;

    /** 触发锁定后的锁定时长（分钟） */
    private int loginLockMinutes = 15;
}
