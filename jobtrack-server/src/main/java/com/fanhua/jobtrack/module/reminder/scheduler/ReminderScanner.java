package com.fanhua.jobtrack.module.reminder.scheduler;

import com.fanhua.jobtrack.module.reminder.service.ReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScanner {
    private final ReminderService reminderService;

    public ReminderScanner(ReminderService reminderService) { this.reminderService = reminderService; }

    @Scheduled(fixedDelayString = "${jobtrack.reminder.scan-interval-ms:30000}")
    public void scan() { reminderService.scanOnce(); }
}
