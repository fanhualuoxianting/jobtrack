package com.fanhua.jobtrack.module.reminder.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.module.interview.entity.Interview;
import com.fanhua.jobtrack.module.reminder.domain.NotificationReadyEvent;
import com.fanhua.jobtrack.module.reminder.domain.ReminderType;
import com.fanhua.jobtrack.module.reminder.dto.NotificationQueryRequest;
import com.fanhua.jobtrack.module.reminder.entity.ReminderRecord;
import com.fanhua.jobtrack.module.reminder.mapper.ReminderMapper;
import com.fanhua.jobtrack.module.reminder.service.ReminderService;
import com.fanhua.jobtrack.module.reminder.service.ReminderDeliveryService;
import com.fanhua.jobtrack.module.reminder.vo.ReminderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private final ReminderMapper reminderMapper;
    private final ReminderDeliveryService deliveryService;

    public ReminderServiceImpl(ReminderMapper reminderMapper, ReminderDeliveryService deliveryService) {
        this.reminderMapper = reminderMapper;
        this.deliveryService = deliveryService;
    }

    @Override
    @Transactional
    public void rebuildForInterview(Long userId, Interview interview) {
        cancelPendingForInterview(userId, interview.getId());
        LocalDateTime start = interview.getScheduledStart();
        LocalDateTime now = LocalDateTime.now(UTC);
        insertIfFuture(userId, interview, ReminderType.INTERVIEW_24_HOURS, start.minusHours(24), now);
        insertIfFuture(userId, interview, ReminderType.INTERVIEW_1_HOUR, start.minusHours(1), now);
    }

    private void insertIfFuture(Long userId, Interview interview, ReminderType type,
                                LocalDateTime scheduledAt, LocalDateTime now) {
        if (!scheduledAt.isAfter(now)) return;
        String key = "INTERVIEW:" + interview.getId() + ":" + type.name() + ":" + scheduledAt;
        reminderMapper.insertIgnore(userId, interview.getId(), interview.getApplicationId(), type.name(), scheduledAt,
                "面试提醒：" + interview.getRoundName(),
                "面试时间：" + interview.getScheduledStart() + " UTC，请提前准备。", key);
    }

    @Override
    @Transactional
    public void cancelPendingForInterview(Long userId, Long interviewId) {
        reminderMapper.cancelPending(userId, interviewId);
    }

    @Override
    public PageResult<ReminderVO> notifications(Long userId, NotificationQueryRequest request) {
        Page<ReminderRecord> page = reminderMapper.selectNotifications(
                new Page<>(request.safePage(), request.safePageSize()), userId, request.safeReadState());
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public long unreadCount(Long userId) { return reminderMapper.countUnread(userId); }

    @Override
    @Transactional
    public ReminderVO markRead(Long userId, Long id) {
        ReminderRecord record = reminderMapper.selectOwned(userId, id);
        if (record == null || !"SENT".equals(record.getStatus())) {
            throw new NotFoundException("通知不存在");
        }
        reminderMapper.markRead(userId, id);
        record.setReadAt(record.getReadAt() == null ? LocalDateTime.now(UTC) : record.getReadAt());
        return toVO(record);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) { reminderMapper.markAllRead(userId); }

    @Override
    public void scanOnce() {
        List<Long> ids = reminderMapper.selectDueIds(100);
        for (Long id : ids) {
            try { deliveryService.processOne(id); } catch (Exception ignored) {
                // 单条提醒失败不能阻断同一批次其他提醒；READY 记录由失败分支保留重试依据。
            }
        }
    }

    private ReminderVO toVO(ReminderRecord source) {
        ReminderVO vo = new ReminderVO();
        vo.setId(source.getId()); vo.setInterviewId(source.getInterviewId()); vo.setApplicationId(source.getApplicationId());
        vo.setReminderType(source.getReminderType()); vo.setStatus(source.getStatus()); vo.setTitle(source.getTitle());
        vo.setContent(source.getContent()); vo.setIdempotencyKey(source.getIdempotencyKey());
        vo.setScheduledAt(atUtc(source.getScheduledAt())); vo.setSentAt(atUtc(source.getSentAt()));
        vo.setReadAt(atUtc(source.getReadAt())); vo.setCancelledAt(atUtc(source.getCancelledAt()));
        vo.setCreatedAt(atUtc(source.getCreatedAt()));
        return vo;
    }

    private OffsetDateTime atUtc(LocalDateTime value) { return value == null ? null : value.atOffset(UTC); }
}
