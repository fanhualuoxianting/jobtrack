package com.fanhua.jobtrack.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求：account 支持用户名或邮箱
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "用户名或邮箱", example = "demo@jobtrack.local", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    @Size(max = 120, message = "账号长度不能超过 120")
    private String account;

    @Schema(description = "密码", example = "JobTrack@123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;

    @Schema(description = "设备名称，便于多设备会话管理", example = "Chrome on Windows")
    @Size(max = 120, message = "设备名称不能超过 120")
    private String deviceName;
}
