package com.fanhua.jobtrack.module.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 普通编辑 DTO 不暴露 status/userId/companyId/positionId/deleted/archived。 */
@Data
public class ApplicationUpdateRequest {
    @NotNull
    private Integer version;
    private Long resumeId;
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
    private String currency;
    @Size(max = 255)
    private String nextAction;
    private OffsetDateTime nextActionAt;
    @Size(max = 5000)
    private String note;
}
