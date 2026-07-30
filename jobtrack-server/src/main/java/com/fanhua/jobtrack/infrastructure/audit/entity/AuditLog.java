package com.fanhua.jobtrack.infrastructure.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务操作审计日志：登录、令牌刷新、会话注销及后续业务关键动作
 */
@Data
@TableName("jt_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID，匿名操作（如登录失败）为 NULL */
    private Long userId;

    /** 动作标识，见 AuditAction 常量 */
    private String action;

    private String resourceType;

    private String resourceId;

    /** SUCCESS / FAIL */
    private String result;

    /** 附加说明，写入前必须脱敏 */
    private String detail;

    private String ipAddress;

    private String userAgent;

    private String traceId;

    private LocalDateTime createdAt;
}
