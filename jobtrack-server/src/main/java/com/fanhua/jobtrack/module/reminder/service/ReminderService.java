package com.fanhua.jobtrack.module.reminder.service;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.module.interview.entity.Interview;
import com.fanhua.jobtrack.module.reminder.dto.NotificationQueryRequest;
import com.fanhua.jobtrack.module.reminder.vo.ReminderVO;

public interface ReminderService {
    void rebuildForInterview(Long userId, Interview interview);
    void cancelPendingForInterview(Long userId, Long interviewId);
    PageResult<ReminderVO> notifications(Long userId, NotificationQueryRequest request);
    long unreadCount(Long userId);
    ReminderVO markRead(Long userId, Long id);
    void markAllRead(Long userId);
    void scanOnce();
}
