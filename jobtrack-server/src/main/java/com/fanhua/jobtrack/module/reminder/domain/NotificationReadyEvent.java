package com.fanhua.jobtrack.module.reminder.domain;

public record NotificationReadyEvent(Long userId, Long reminderId, String title, String content) {
}
