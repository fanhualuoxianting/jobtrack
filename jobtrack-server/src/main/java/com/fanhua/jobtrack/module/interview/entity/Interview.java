package com.fanhua.jobtrack.module.interview.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jt_interview")
public class Interview {
    @TableId
    private Long id;
    private Long userId;
    private Long applicationId;
    private Integer roundNo;
    private String title;
    private String roundName;
    private String interviewType;
    private String status;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private String timezone;
    private String location;
    private String meetingUrl;
    private String interviewer;
    private String contactInfo;
    private String result;
    private String feedback;
    private String note;
    private String cancelReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
    private Integer version;
}
