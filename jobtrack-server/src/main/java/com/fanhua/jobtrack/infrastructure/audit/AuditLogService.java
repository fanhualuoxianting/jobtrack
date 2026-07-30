package com.fanhua.jobtrack.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 业务审计日志服务。
 *
 * 两种写入时机：
 * - record：立即异步写入。用于"动作确实发生了"的失败/攻击类审计（登录失败、
 *   令牌重放等），与业务事务成败无关。
 * - recordAfterCommit：当前事务提交成功后才写入。用于成功类审计（创建、修改、
 *   登录成功等），避免业务回滚后仍留下"操作成功"的假记录；
 *   无活动事务时退化为立即异步写入。
 */
@Slf4j
@Service
public class AuditLogService {

    private final AuditLogWriter writer;

    public AuditLogService(AuditLogWriter writer) {
        this.writer = writer;
    }

    /** 事务提交后记录成功类审计 */
    public void recordAfterCommit(String action, Long userId, String resourceType, String resourceId,
                                  boolean success, String detail, String ip, String userAgent, String traceId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    writer.writeAsync(action, userId, resourceType, resourceId, success, detail, ip, userAgent, traceId);
                }
            });
        } else {
            writer.writeAsync(action, userId, resourceType, resourceId, success, detail, ip, userAgent, traceId);
        }
    }

    /** 立即异步记录（失败/攻击类事件；不带事务关联） */
    public void record(String action, Long userId, String resourceType, String resourceId,
                       boolean success, String detail, HttpServletRequest request) {
        writer.writeAsync(action, userId, resourceType, resourceId, success, detail,
                request == null ? null : ClientIpUtil.getClientIp(request),
                request == null ? null : ClientIpUtil.getUserAgent(request),
                MDC.get("traceId"));
    }

    /** 立即异步记录 */
    public void record(String action, Long userId, String resourceType, String resourceId,
                       boolean success, String detail, String ip, String userAgent, String traceId) {
        writer.writeAsync(action, userId, resourceType, resourceId, success, detail, ip, userAgent, traceId);
    }
}
