package com.fanhua.jobtrack.module.application.event;

import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatusChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 成功审计只在主事务提交后写入，回滚不会留下“流转成功”记录。 */
@Component
public class ApplicationStatusChangedEventListener {

    private final AuditLogService auditLogService;

    public ApplicationStatusChangedEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        auditLogService.record("APPLICATION_STATUS_CHANGED", event.userId(), "APPLICATION",
                String.valueOf(event.applicationId()), true,
                event.fromStatus() + " -> " + event.toStatus()
                        + (event.reason() == null || event.reason().isBlank() ? "" : ": " + event.reason()),
                null, null, event.traceId());
    }
}
