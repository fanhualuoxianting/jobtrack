package com.fanhua.jobtrack.module.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InterviewCreateRequest {
    @NotNull
    private Long applicationId;
    @NotNull @Positive
    private Integer roundNumber;
    @NotBlank @Size(max = 150)
    private String roundName;
    @NotBlank @Pattern(regexp = "^(PHONE|VIDEO|ONSITE|WRITTEN|OTHER)$")
    private String interviewType;
    @NotNull
    private OffsetDateTime scheduledStartAt;
    @NotNull
    private OffsetDateTime scheduledEndAt;
    @NotBlank @Size(max = 50)
    private String timezone;
    @Size(max = 500)
    private String location;
    @Pattern(regexp = "^$|^https?://\\S+$", message = "会议链接必须是合法 http/https 地址")
    private String meetingUrl;
    @Size(max = 200)
    private String interviewer;
    @Size(max = 500)
    private String contactInfo;
    @Size(max = 5000)
    private String notes;
}
