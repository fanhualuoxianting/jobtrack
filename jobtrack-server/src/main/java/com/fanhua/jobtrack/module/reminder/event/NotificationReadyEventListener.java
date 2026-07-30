package com.fanhua.jobtrack.module.reminder.event;

import com.fanhua.jobtrack.module.reminder.domain.NotificationReadyEvent;
import com.fanhua.jobtrack.module.reminder.stream.NotificationStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class NotificationReadyEventListener {
    private final NotificationStreamService streamService;

    public NotificationReadyEventListener(NotificationStreamService streamService) {
        this.streamService = streamService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReady(NotificationReadyEvent event) {
        try {
            streamService.send(event.userId(), event.reminderId(), event.title(), event.content());
        } catch (RuntimeException ex) {
            log.warn("通知 SSE 投递失败，数据库通知仍保留: reminderId={}", event.reminderId(), ex);
        }
    }
}
