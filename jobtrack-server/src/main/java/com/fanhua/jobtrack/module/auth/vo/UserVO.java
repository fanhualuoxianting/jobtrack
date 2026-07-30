package com.fanhua.jobtrack.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户信息视图对象，绝不包含密码字段
 */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "demo")
    private String username;

    @Schema(description = "邮箱", example = "demo@jobtrack.local")
    private String email;

    @Schema(description = "昵称", example = "演示用户")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "角色", example = "USER")
    private String role;

    @Schema(description = "最后登录时间")
    private OffsetDateTime lastLoginAt;
}
