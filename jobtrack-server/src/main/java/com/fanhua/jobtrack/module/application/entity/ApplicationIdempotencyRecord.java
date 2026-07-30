package com.fanhua.jobtrack.module.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jt_application_idempotency")
public class ApplicationIdempotencyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long applicationId;
    private String idempotencyKey;
    private String requestHash;
    private String targetStatus;
    private String reason;
    private String resultStatus;
    private Integer resultVersion;
    private String resultToStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
