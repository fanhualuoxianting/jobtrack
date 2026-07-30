package com.fanhua.jobtrack.module.interview.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InterviewCompleteRequest {
    @NotNull
    private Integer version;
    @jakarta.validation.constraints.Pattern(regexp = "^$|^(PASS|FAIL|PENDING|UNKNOWN|NO_SHOW)$")
    private String result;
    @Size(max = 5000)
    private String feedback;
}
