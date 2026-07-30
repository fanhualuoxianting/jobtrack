package com.fanhua.jobtrack.module.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationTransitionRequest {
    @NotBlank
    private String targetStatus;
    @Size(max = 500)
    private String reason;
    @NotNull
    private Integer expectedVersion;
    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;
}
