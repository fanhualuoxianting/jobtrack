package com.fanhua.jobtrack.module.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 岗位新增/修改请求
 */
@Data
@Schema(description = "岗位保存请求")
public class PositionUpsertRequest {

    @Schema(description = "所属公司ID（必须属于当前用户）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "公司不能为空")
    private Long companyId;

    @Schema(description = "岗位名称", example = "Java 后端开发实习生", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 150)
    private String title;

    @Schema(description = "部门")
    @Size(max = 100)
    private String department;

    @Schema(description = "工作城市")
    @Size(max = 80)
    private String city;

    @Schema(description = "工作类型", example = "INTERNSHIP")
    @NotBlank(message = "工作类型不能为空")
    @Pattern(regexp = "^(INTERNSHIP|FULL_TIME|PART_TIME)$", message = "工作类型不合法")
    private String workType;

    @Schema(description = "办公方式", example = "ONSITE")
    @NotBlank(message = "办公方式不能为空")
    @Pattern(regexp = "^(ONSITE|REMOTE|HYBRID)$", message = "办公方式不合法")
    private String workplaceType;

    @Schema(description = "薪资下限")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    private BigDecimal salaryMin;

    @Schema(description = "薪资上限")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    private BigDecimal salaryMax;

    @Schema(description = "薪资单位 DAY/MONTH/YEAR")
    @Pattern(regexp = "^$|^(DAY|MONTH|YEAR)$", message = "薪资单位不合法")
    private String salaryUnit;

    @Schema(description = "币种", defaultValue = "CNY")
    @Size(max = 10)
    private String currency;

    @Schema(description = "招聘来源，如 BOSS/官网/内推")
    @Size(max = 50)
    private String source;

    @Schema(description = "岗位链接（若提供必须是合法 URL）")
    @Size(max = 1000)
    @Pattern(regexp = "^$|^https?://[\\w\\-.]+(?::\\d+)?(/\\S*)?$", message = "岗位链接必须是合法 URL")
    private String sourceUrl;

    @Schema(description = "岗位描述")
    @Size(max = 10000)
    private String description;

    @Schema(description = "岗位要求")
    @Size(max = 10000)
    private String requirements;

    @Schema(description = "发布时间")
    private OffsetDateTime publishedAt;

    @Schema(description = "截止时间（不得早于发布时间）")
    private OffsetDateTime deadlineAt;
}
