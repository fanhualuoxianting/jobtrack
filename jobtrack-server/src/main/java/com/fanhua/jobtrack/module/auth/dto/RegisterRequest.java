package com.fanhua.jobtrack.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "用户名：4-50 位字母、数字或下划线", example = "fanhua", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,50}$", message = "用户名需为 4-50 位字母、数字或下划线")
    private String username;

    @Schema(description = "邮箱", example = "fanhua@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不合法")
    @Size(max = 120, message = "邮箱长度不能超过 120")
    private String email;

    @Schema(description = "密码：8-64 位，至少包含字母和数字", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;

    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "昵称，缺省取用户名", example = "繁花")
    @Size(max = 60, message = "昵称长度不能超过 60")
    private String nickname;
}
