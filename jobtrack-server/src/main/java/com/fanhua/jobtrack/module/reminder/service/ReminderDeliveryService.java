package com.fanhua.jobtrack.module.reminder.service;

import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.module.reminder.domain.NotificationReadyEvent;
import com.fanhua.jobtrack.module.reminder.entity.ReminderRecord;
import com.fanhua.jobtrack.module.reminder.mapper.ReminderMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderDeliveryService {
    private final ReminderMapper reminderMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ReminderDeliveryService(ReminderMapper reminderMapper, ApplicationEventPublisher eventPublisher) {
        this.reminderMapper = reminderMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processOne(Long id) {
        if (reminderMapper.claim(id) == 0) return;
        ReminderRecord ready = reminderMapper.selectReady(id);
        if (ready == null) return;
        try {
            if (reminderMapper.markSent(id) == 1) {
                eventPublisher.publishEvent(new NotificationReadyEvent(
                        ready.getUserId(), ready.getId(), ready.getTitle(), ready.getContent()));
            }
        } catch (RuntimeException ex) {
            reminderMapper.markFailed(id, safeMessage(ex));
            // 失败状态本身要提交，下一次扫描依据 retry_count 决定重试或 FAILED。
        }
    }

    private String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null ? ErrorCode.INTERNAL_ERROR.getDefaultMessage()
                : message.substring(0, Math.min(500, message.length()));
    }
}
