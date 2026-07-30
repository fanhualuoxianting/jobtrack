package com.fanhua.jobtrack.module.reminder;

import com.fanhua.jobtrack.module.reminder.entity.ReminderRecord;
import com.fanhua.jobtrack.module.reminder.mapper.ReminderMapper;
import com.fanhua.jobtrack.module.reminder.service.ReminderDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderDeliveryServiceTest {
    @Mock private ReminderMapper reminderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void failedNotificationDeliveryRecordsRetryCauseWithoutEscapingBatch() {
        ReminderRecord ready = new ReminderRecord();
        ready.setId(7L);
        ready.setUserId(11L);
        ready.setTitle("面试提醒");
        ready.setContent("请提前准备");
        when(reminderMapper.claim(7L)).thenReturn(1);
        when(reminderMapper.selectReady(7L)).thenReturn(ready);
        when(reminderMapper.markSent(7L)).thenReturn(1);
        doThrow(new IllegalStateException("delivery channel unavailable"))
                .when(eventPublisher).publishEvent(any(Object.class));

        new ReminderDeliveryService(reminderMapper, eventPublisher).processOne(7L);

        verify(reminderMapper).markSent(7L);
        verify(reminderMapper).markFailed(eq(7L), contains("delivery channel unavailable"));
    }
}
