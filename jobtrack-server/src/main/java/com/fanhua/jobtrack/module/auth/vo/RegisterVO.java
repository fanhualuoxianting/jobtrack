package com.fanhua.jobtrack.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 注册成功响应（注册后需重新登录，不返回令牌）
 */
@Data
@Schema(description = "注册响应")
public class RegisterVO {

    @Schema(description = "用户ID", example = "2")
    private Long userId;

    @Schema(description = "用户名", example = "fanhua")
    private String username;

    @Schema(description = "邮箱", example = "fanhua@example.com")
    private String email;

    @Schema(description = "昵称", example = "繁花")
    private String nickname;
}
