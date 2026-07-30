package com.fanhua.jobtrack.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatus;
import com.fanhua.jobtrack.module.application.dto.ApplicationTransitionRequest;
import com.fanhua.jobtrack.module.application.entity.JobApplication;
import com.fanhua.jobtrack.module.application.mapper.JobApplicationMapper;
import com.fanhua.jobtrack.module.application.service.ApplicationService;
import com.fanhua.jobtrack.module.interview.domain.InterviewStatus;
import com.fanhua.jobtrack.module.interview.dto.InterviewCancelRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCompleteRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewCreateRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewQueryRequest;
import com.fanhua.jobtrack.module.interview.dto.InterviewUpdateRequest;
import com.fanhua.jobtrack.module.interview.entity.Interview;
import com.fanhua.jobtrack.module.interview.mapper.InterviewMapper;
import com.fanhua.jobtrack.module.interview.service.InterviewService;
import com.fanhua.jobtrack.module.interview.vo.InterviewVO;
import com.fanhua.jobtrack.module.reminder.service.ReminderService;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class InterviewServiceImpl implements InterviewService {
    private static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;
    private static final EnumSet<ApplicationStatus> ALLOWED_APPLICATION_STATUSES =
            EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.ASSESSMENT, ApplicationStatus.INTERVIEWING);

    private final InterviewMapper interviewMapper;
    private final JobApplicationMapper applicationMapper;
    private final ApplicationService applicationService;
    private final ReminderService reminderService;
    private final AuditLogService auditLogService;

    public InterviewServiceImpl(InterviewMapper interviewMapper, JobApplicationMapper applicationMapper,
                                ApplicationService applicationService, ReminderService reminderService,
                                AuditLogService auditLogService) {
        this.interviewMapper = interviewMapper;
        this.applicationMapper = applicationMapper;
        this.applicationService = applicationService;
        this.reminderService = reminderService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public InterviewVO create(Long userId, InterviewCreateRequest request) {
        JobApplication application = ownedApplication(userId, request.getApplicationId());
        if (application.getArchived() != null && application.getArchived() == 1) {
            throw new ConflictException(ErrorCode.INTERVIEW_NOT_ALLOWED.getCode(), "归档投递不能安排面试");
        }
        ApplicationStatus currentStatus = ApplicationStatus.valueOf(application.getStatus());
        if (!ALLOWED_APPLICATION_STATUSES.contains(currentStatus)) {
            throw new ConflictException(ErrorCode.INTERVIEW_NOT_ALLOWED.getCode(),
                    "当前投递状态不允许安排面试");
        }
        validateTime(request.getScheduledStartAt(), request.getScheduledEndAt(), request.getTimezone());
        if (interviewMapper.countByRound(userId, request.getApplicationId(), request.getRoundNumber()) > 0) {
            throw new ConflictException(ErrorCode.INTERVIEW_DUPLICATE_ROUND.getCode(),
                    ErrorCode.INTERVIEW_DUPLICATE_ROUND.getDefaultMessage());
        }

        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setApplicationId(request.getApplicationId());
        interview.setRoundNo(request.getRoundNumber());
        interview.setRoundName(trim(request.getRoundName()));
        interview.setTitle(trim(request.getRoundName()));
        interview.setInterviewType(request.getInterviewType());
        interview.setStatus(InterviewStatus.SCHEDULED.name());
        interview.setScheduledStart(toUtc(request.getScheduledStartAt()));
        interview.setScheduledEnd(toUtc(request.getScheduledEndAt()));
        interview.setTimezone(request.getTimezone().trim());
        interview.setLocation(trim(request.getLocation()));
        interview.setMeetingUrl(trim(request.getMeetingUrl()));
        interview.setInterviewer(trim(request.getInterviewer()));
        interview.setContactInfo(trim(request.getContactInfo()));
        interview.setNote(trim(request.getNotes()));
        try {
            interviewMapper.insert(interview);
        } catch (DuplicateKeyException e) {
            throw new ConflictException(ErrorCode.INTERVIEW_DUPLICATE_ROUND.getCode(),
                    ErrorCode.INTERVIEW_DUPLICATE_ROUND.getDefaultMessage());
        }

        if (currentStatus == ApplicationStatus.APPLIED || currentStatus == ApplicationStatus.ASSESSMENT) {
            ApplicationTransitionRequest transition = new ApplicationTransitionRequest();
            transition.setTargetStatus(ApplicationStatus.INTERVIEWING.name());
            transition.setExpectedVersion(application.getVersion());
            transition.setIdempotencyKey("interview-auto-" + interview.getId() + "-" + UUID.randomUUID());
            applicationService.transition(userId, application.getId(), transition);
        }
        reminderService.rebuildForInterview(userId, interview);
        auditLogService.recordAfterCommit("INTERVIEW_CREATE", userId, "INTERVIEW",
                String.valueOf(interview.getId()), true, "创建面试", null, null, MDC.get("traceId"));
        return toVO(interviewMapper.selectOwned(userId, interview.getId()));
    }

    @Override
    public PageResult<InterviewVO> page(Long userId, InterviewQueryRequest request) {
        Page<Interview> page = interviewMapper.selectPage(new Page<>(request.safePage(), request.safePageSize()),
                userId, request.safeStatus(), request.getCompanyId(), request.getApplicationId(),
                request.getFrom() == null ? null : toUtc(request.getFrom()),
                request.getTo() == null ? null : toUtc(request.getTo()));
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public InterviewVO detail(Long userId, Long id) {
        return toVO(ownedInterview(userId, id));
    }

    @Override
    public List<InterviewVO> listByApplication(Long userId, Long applicationId) {
        ownedApplication(userId, applicationId);
        return interviewMapper.selectByApplication(userId, applicationId).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public InterviewVO update(Long userId, Long id, InterviewUpdateRequest request) {
        Interview current = ownedInterview(userId, id);
        if (!InterviewStatus.SCHEDULED.name().equals(current.getStatus())) {
            throw statusConflict();
        }
        String timezone = request.getTimezone() == null ? current.getTimezone() : request.getTimezone().trim();
        OffsetDateTime start = request.getScheduledStartAt() == null
                ? fromUtc(current.getScheduledStart(), current.getTimezone()) : request.getScheduledStartAt();
        OffsetDateTime end = request.getScheduledEndAt() == null
                ? fromUtc(current.getScheduledEnd(), current.getTimezone()) : request.getScheduledEndAt();
        validateTime(start, end, timezone);
        int affected = interviewMapper.updateEditable(userId, id, request.getVersion(), trim(request.getInterviewType()),
                request.getScheduledStartAt() == null ? null : toUtc(start),
                request.getScheduledEndAt() == null ? null : toUtc(end),
                request.getTimezone() == null ? null : timezone,
                trim(request.getLocation()), trim(request.getMeetingUrl()), trim(request.getInterviewer()),
                trim(request.getContactInfo()), trim(request.getNotes()));
        if (affected == 0) throw updateConflict(userId, id, request.getVersion());
        Interview updated = interviewMapper.selectOwned(userId, id);
        reminderService.rebuildForInterview(userId, updated);
        auditLogService.recordAfterCommit("INTERVIEW_UPDATE", userId, "INTERVIEW", String.valueOf(id), true,
                "修改面试", null, null, MDC.get("traceId"));
        return toVO(updated);
    }

    @Override
    @Transactional
    public InterviewVO cancel(Long userId, Long id, InterviewCancelRequest request) {
        Interview current = ownedInterview(userId, id);
        int affected = interviewMapper.cancel(userId, id, request.getVersion(), trim(request.getReason()));
        if (affected == 0) throw updateConflict(userId, id, request.getVersion());
        reminderService.cancelPendingForInterview(userId, id);
        auditLogService.recordAfterCommit("INTERVIEW_CANCEL", userId, "INTERVIEW", String.valueOf(id), true,
                "取消面试", null, null, MDC.get("traceId"));
        return toVO(interviewMapper.selectOwned(userId, id));
    }

    @Override
    @Transactional
    public InterviewVO complete(Long userId, Long id, InterviewCompleteRequest request) {
        Interview current = ownedInterview(userId, id);
        int affected = interviewMapper.complete(userId, id, request.getVersion(), trim(request.getResult()),
                trim(request.getFeedback()));
        if (affected == 0) throw updateConflict(userId, id, request.getVersion());
        reminderService.cancelPendingForInterview(userId, id);
        auditLogService.recordAfterCommit("INTERVIEW_COMPLETE", userId, "INTERVIEW", String.valueOf(id), true,
                "完成面试", null, null, MDC.get("traceId"));
        return toVO(interviewMapper.selectOwned(userId, id));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id, Integer version) {
        ownedInterview(userId, id);
        if (interviewMapper.softDelete(userId, id, version) == 0) throw updateConflict(userId, id, version);
        reminderService.cancelPendingForInterview(userId, id);
        auditLogService.recordAfterCommit("INTERVIEW_DELETE", userId, "INTERVIEW", String.valueOf(id), true,
                "删除面试", null, null, MDC.get("traceId"));
    }

    private JobApplication ownedApplication(Long userId, Long id) {
        JobApplication application = applicationMapper.selectOwned(userId, id);
        if (application == null) throw new NotFoundException("投递记录不存在");
        return application;
    }

    private Interview ownedInterview(Long userId, Long id) {
        Interview interview = interviewMapper.selectOwned(userId, id);
        if (interview == null) throw new NotFoundException("面试记录不存在");
        return interview;
    }

    private ConflictException statusConflict() {
        return new ConflictException(ErrorCode.INTERVIEW_STATUS_INVALID.getCode(),
                ErrorCode.INTERVIEW_STATUS_INVALID.getDefaultMessage());
    }

    private ConflictException updateConflict(Long userId, Long id, Integer version) {
        Interview current = interviewMapper.selectOwned(userId, id);
        if (current == null) throw new NotFoundException("面试记录不存在");
        if (!InterviewStatus.SCHEDULED.name().equals(current.getStatus())) throw statusConflict();
        return new ConflictException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
                "面试记录版本已变化，请刷新后重试");
    }

    private void validateTime(OffsetDateTime start, OffsetDateTime end, String timezone) {
        try { ZoneId.of(timezone); } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID.getCode(), "时区值无效");
        }
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException(ErrorCode.INTERVIEW_TIME_INVALID.getCode(), "面试开始时间必须早于结束时间");
        }
    }

    private LocalDateTime toUtc(OffsetDateTime value) {
        return value.toInstant().atOffset(STORAGE_ZONE).toLocalDateTime();
    }

    private OffsetDateTime fromUtc(LocalDateTime value, String timezone) {
        if (value == null) return null;
        ZonedDateTime local = value.atOffset(STORAGE_ZONE).atZoneSameInstant(ZoneId.of(timezone));
        return local.toOffsetDateTime();
    }

    private InterviewVO toVO(Interview source) {
        InterviewVO vo = new InterviewVO();
        vo.setId(source.getId()); vo.setApplicationId(source.getApplicationId()); vo.setRoundNumber(source.getRoundNo());
        vo.setRoundName(source.getRoundName()); vo.setInterviewType(source.getInterviewType()); vo.setTimezone(source.getTimezone());
        OffsetDateTime scheduledStart = fromUtc(source.getScheduledStart(), source.getTimezone());
        OffsetDateTime scheduledEnd = fromUtc(source.getScheduledEnd(), source.getTimezone());
        vo.setScheduledStartAt(scheduledStart == null ? null : scheduledStart.toString());
        vo.setScheduledEndAt(scheduledEnd == null ? null : scheduledEnd.toString());
        vo.setLocation(source.getLocation()); vo.setMeetingUrl(source.getMeetingUrl()); vo.setInterviewer(source.getInterviewer());
        vo.setContactInfo(source.getContactInfo()); vo.setNotes(source.getNote()); vo.setStatus(source.getStatus());
        vo.setResult(source.getResult()); vo.setFeedback(source.getFeedback()); vo.setCancelReason(source.getCancelReason());
        if (source.getCreatedAt() != null) vo.setCreatedAt(fromUtc(source.getCreatedAt(), source.getTimezone()));
        if (source.getUpdatedAt() != null) vo.setUpdatedAt(fromUtc(source.getUpdatedAt(), source.getTimezone()));
        vo.setVersion(source.getVersion());
        return vo;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
