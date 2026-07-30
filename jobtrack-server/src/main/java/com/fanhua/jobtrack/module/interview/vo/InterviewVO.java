package com.fanhua.jobtrack.module.interview.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class InterviewVO {
    private Long id;
    private Long applicationId;
    private Integer roundNumber;
    private String roundName;
    private String interviewType;
    private String scheduledStartAt;
    private String scheduledEndAt;
    private String timezone;
    private String location;
    private String meetingUrl;
    private String interviewer;
    private String contactInfo;
    private String notes;
    private String status;
    private String result;
    private String feedback;
    private String cancelReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;
}
