package com.fanhua.jobtrack.module.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InterviewCancelRequest {
    @NotNull
    private Integer version;
    @NotBlank @Size(max = 500)
    private String reason;
}
