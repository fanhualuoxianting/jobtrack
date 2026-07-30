package com.fanhua.jobtrack.module.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Schema(description = "创建投递请求，初始状态固定为 SAVED")
public class ApplicationCreateRequest {
    @NotNull
    private Long companyId;
    @NotNull
    private Long positionId;
    private Long resumeId;
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "优先级不合法")
    private String priority = "MEDIUM";
    @Size(max = 50)
    private String source;
    private OffsetDateTime appliedAt;
    @Size(max = 100)
    private String referralName;
    @DecimalMin(value = "0", message = "期望薪资不能为负数")
    private BigDecimal expectedSalaryMin;
    @DecimalMin(value = "0", message = "期望薪资不能为负数")
    private BigDecimal expectedSalaryMax;
    @Size(max = 10)
    private String currency = "CNY";
    @Size(max = 255)
    private String nextAction;
    private OffsetDateTime nextActionAt;
    @Size(max = 5000)
    private String note;
}
