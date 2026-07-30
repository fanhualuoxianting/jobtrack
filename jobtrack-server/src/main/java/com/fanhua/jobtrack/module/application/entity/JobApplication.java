package com.fanhua.jobtrack.module.application.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("jt_job_application")
public class JobApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long companyId;
    private Long positionId;
    private Long resumeId;
    private String status;
    private String priority;
    private String source;
    private LocalDateTime appliedAt;
    private String referralName;
    private BigDecimal expectedSalaryMin;
    private BigDecimal expectedSalaryMax;
    private String currency;
    private String nextAction;
    private LocalDateTime nextActionAt;
    private String note;
    private Integer archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
    @Version
    private Integer version;
}
