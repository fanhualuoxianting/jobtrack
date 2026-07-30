package com.fanhua.jobtrack.module.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jt_application_status_log")
public class ApplicationStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long operatorUserId;
    private Long applicationId;
    private String fromStatus;
    private String toStatus;
    private String changeNote;
    private LocalDateTime changedAt;
    private String traceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
