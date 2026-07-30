package com.fanhua.jobtrack.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，密钥通过环境变量注入
 */
@Data
@Component
@ConfigurationProperties(prefix = "jobtrack.jwt")
public class JwtProperties {

    /** HMAC 密钥，至少 32 字节；生产必须由 JWT_SECRET 环境变量注入 */
    private String secret;

    /** Access Token 有效期（分钟） */
    private long accessTtlMinutes = 30;

    /** Refresh Token 有效期（天） */
    private long refreshTtlDays = 7;
}
