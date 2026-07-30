package com.fanhua.jobtrack.module.reminder.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jt_reminder")
public class ReminderRecord {
    @TableId
    private Long id;
    private Long userId;
    private Long interviewId;
    private Long applicationId;
    private String reminderType;
    private LocalDateTime scheduledAt;
    private String status;
    private String title;
    private String content;
    private String idempotencyKey;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
    private Integer retryCount;
    private Integer maxRetries;
    private String lastError;
}
