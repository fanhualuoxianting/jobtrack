package com.fanhua.jobtrack.infrastructure.audit;

import com.fanhua.jobtrack.infrastructure.audit.entity.AuditLog;
import com.fanhua.jobtrack.infrastructure.audit.mapper.AuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步写入器（独立 Bean，保证 @Async 经代理生效）。
 * 失败只记 ERROR，绝不影响主业务。
 */
@Slf4j
@Component
public class AuditLogWriter {

    private final AuditLogMapper auditLogMapper;

    public AuditLogWriter(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Async
    public void writeAsync(String action, Long userId, String resourceType, String resourceId,
                           boolean success, String detail, String ip, String userAgent, String traceId) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setUserId(userId);
            logEntry.setAction(action);
            logEntry.setResourceType(resourceType);
            logEntry.setResourceId(resourceId);
            logEntry.setResult(success ? "SUCCESS" : "FAIL");
            logEntry.setDetail(truncate(detail, 500));
            logEntry.setIpAddress(ip);
            logEntry.setUserAgent(truncate(userAgent, 500));
            logEntry.setTraceId(traceId);
            auditLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("审计日志写入失败: action={}, userId={}, error={}", action, userId, e.getMessage());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
