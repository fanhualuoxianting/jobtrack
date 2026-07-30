package com.fanhua.jobtrack.module.company.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公司新增/修改请求
 */
@Data
@Schema(description = "公司保存请求")
public class CompanyUpsertRequest {

    @Schema(description = "公司名称（必填，自动去除首尾空格）", example = "示例科技有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 150, message = "公司名称不能超过 150 字符")
    private String name;

    @Schema(description = "简称")
    @Size(max = 80)
    private String shortName;

    @Schema(description = "行业")
    @Size(max = 80)
    private String industry;

    @Schema(description = "规模，如 500-999人")
    @Size(max = 30)
    private String scale;

    @Schema(description = "所在城市")
    @Size(max = 80)
    private String city;

    @Schema(description = "官网地址（若提供必须是合法 http/https URL）")
    @Size(max = 500)
    @Pattern(regexp = "^$|^https?://[\\w\\-.]+(?::\\d+)?(/\\S*)?$", message = "官网地址必须是合法 URL")
    private String website;

    @Schema(description = "备注")
    @Size(max = 2000)
    private String description;
}
