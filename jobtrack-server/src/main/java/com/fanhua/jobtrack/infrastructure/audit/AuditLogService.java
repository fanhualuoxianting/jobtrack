package com.fanhua.jobtrack.infrastructure.audit;

import com.fanhua.jobtrack.infrastructure.audit.entity.AuditLog;
import com.fanhua.jobtrack.infrastructure.audit.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 业务审计日志服务：异步落库、尽力而为。
 * 审计写入失败只记录 ERROR，绝不影响主业务事务。
 */
@Slf4j
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录一条审计日志。
     * 注意：异步线程不共享请求 MDC，traceId 在调用方线程先取出再传入。
     */
    @Async
    public void record(String action, Long userId, String resourceType, String resourceId,
                       boolean success, String detail, HttpServletRequest request) {
        record(action, userId, resourceType, resourceId, success, detail,
                request == null ? null : ClientIpUtil.getClientIp(request),
                request == null ? null : ClientIpUtil.getUserAgent(request),
                MDC.get("traceId"));
    }

    @Async
    public void record(String action, Long userId, String resourceType, String resourceId,
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
