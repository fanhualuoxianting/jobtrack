package com.fanhua.jobtrack.module.reminder.controller;

import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.reminder.dto.NotificationQueryRequest;
import com.fanhua.jobtrack.module.reminder.service.ReminderService;
import com.fanhua.jobtrack.module.reminder.stream.NotificationStreamService;
import com.fanhua.jobtrack.module.reminder.vo.ReminderVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final ReminderService reminderService;
    private final NotificationStreamService streamService;

    public NotificationController(ReminderService reminderService, NotificationStreamService streamService) {
        this.reminderService = reminderService;
        this.streamService = streamService;
    }

    @GetMapping
    public Result<PageResult<ReminderVO>> page(@Valid @ParameterObject NotificationQueryRequest request) {
        return Result.success(reminderService.notifications(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(reminderService.unreadCount(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/{id}/read")
    public Result<ReminderVO> markRead(@PathVariable Long id) {
        return Result.success("已读", reminderService.markRead(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        reminderService.markAllRead(SecurityUtils.currentUserId());
        return Result.success("已全部读", null);
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return streamService.connect(SecurityUtils.currentUserId());
    }
}
