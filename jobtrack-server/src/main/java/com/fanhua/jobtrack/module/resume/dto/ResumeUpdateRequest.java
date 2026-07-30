package com.fanhua.jobtrack.module.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 简历信息修改请求（版本名 / 备注）
 */
@Data
@Schema(description = "简历信息修改")
public class ResumeUpdateRequest {

    @Schema(description = "版本名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "版本名称不能为空")
    @Size(max = 100, message = "版本名称不能超过 100 字符")
    private String versionName;

    @Schema(description = "备注")
    @Size(max = 500)
    private String note;
}
