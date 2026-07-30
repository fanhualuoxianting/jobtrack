package com.fanhua.jobtrack.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录/刷新成功返回的令牌信息
 */
@Data
@Schema(description = "认证令牌响应")
public class AuthTokenVO {

    @Schema(description = "Access Token (JWT)")
    private String accessToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Access Token 有效期（秒）", example = "1800")
    private long expiresIn;

    @Schema(description = "当前用户信息")
    private UserVO user;
}
