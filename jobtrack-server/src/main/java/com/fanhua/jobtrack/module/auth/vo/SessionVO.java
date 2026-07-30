package com.fanhua.jobtrack.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 登录会话视图对象（设备列表）
 */
@Data
@Schema(description = "登录会话")
public class SessionVO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "客户端 User-Agent")
    private String userAgent;

    @Schema(description = "登录 IP")
    private String ipAddress;

    @Schema(description = "创建时间")
    private OffsetDateTime createdAt;

    @Schema(description = "最近活跃时间")
    private OffsetDateTime lastActiveAt;

    @Schema(description = "过期时间")
    private OffsetDateTime expiresAt;

    @Schema(description = "是否为发起请求的当前会话")
    private boolean current;
}
