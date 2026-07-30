package com.fanhua.jobtrack.module.reminder.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ReminderVO {
    private Long id;
    private Long interviewId;
    private Long applicationId;
    private String reminderType;
    private OffsetDateTime scheduledAt;
    private String status;
    private String title;
    private String content;
    private String idempotencyKey;
    private OffsetDateTime sentAt;
    private OffsetDateTime readAt;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
}
