package com.fanhua.jobtrack.module.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.application.domain.ApplicationStateMachine;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatus;
import com.fanhua.jobtrack.module.application.domain.ApplicationStatusChangedEvent;
import com.fanhua.jobtrack.module.application.domain.ApplicationTransitionRule;
import com.fanhua.jobtrack.module.application.dto.ApplicationCreateRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationQueryRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationTransitionRequest;
import com.fanhua.jobtrack.module.application.dto.ApplicationUpdateRequest;
import com.fanhua.jobtrack.module.application.entity.ApplicationIdempotencyRecord;
import com.fanhua.jobtrack.module.application.entity.ApplicationStatusLog;
import com.fanhua.jobtrack.module.application.entity.JobApplication;
import com.fanhua.jobtrack.module.application.mapper.ApplicationIdempotencyMapper;
import com.fanhua.jobtrack.module.application.mapper.ApplicationStatusLogMapper;
import com.fanhua.jobtrack.module.application.mapper.JobApplicationMapper;
import com.fanhua.jobtrack.module.application.service.ApplicationService;
import com.fanhua.jobtrack.module.application.vo.ApplicationListRow;
import com.fanhua.jobtrack.module.application.vo.ApplicationTimelineVO;
import com.fanhua.jobtrack.module.application.vo.ApplicationVO;
import com.fanhua.jobtrack.module.company.entity.Company;
import com.fanhua.jobtrack.module.company.mapper.CompanyMapper;
import com.fanhua.jobtrack.module.position.entity.Position;
import com.fanhua.jobtrack.module.position.mapper.PositionMapper;
import com.fanhua.jobtrack.module.resume.entity.Resume;
import com.fanhua.jobtrack.module.resume.mapper.ResumeMapper;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final JobApplicationMapper applicationMapper;
    private final ApplicationStatusLogMapper statusLogMapper;
    private final ApplicationIdempotencyMapper idempotencyMapper;
    private final CompanyMapper companyMapper;
    private final PositionMapper positionMapper;
    private final ResumeMapper resumeMapper;
    private final ApplicationStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public ApplicationServiceImpl(JobApplicationMapper applicationMapper,
                                  ApplicationStatusLogMapper statusLogMapper,
                                  ApplicationIdempotencyMapper idempotencyMapper,
                                  CompanyMapper companyMapper,
                                  PositionMapper positionMapper,
                                  ResumeMapper resumeMapper,
                                  ApplicationStateMachine stateMachine,
                                  ApplicationEventPublisher eventPublisher,
                                  AuditLogService auditLogService) {
        this.applicationMapper = applicationMapper;
        this.statusLogMapper = statusLogMapper;
        this.idempotencyMapper = idempotencyMapper;
        this.companyMapper = companyMapper;
        this.positionMapper = positionMapper;
        this.resumeMapper = resumeMapper;
        this.stateMachine = stateMachine;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public ApplicationVO create(Long userId, ApplicationCreateRequest request) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getId, request.getCompanyId()).eq(Company::getUserId, userId));
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getId, request.getPositionId()).eq(Position::getUserId, userId));
        if (company == null || position == null) {
            throw new NotFoundException("公司或岗位不存在");
        }
        if (!request.getCompanyId().equals(position.getCompanyId())) {
            throw new NotFoundException("公司或岗位不存在");
        }
        if (!"OPEN".equals(position.getStatus())) {
            throw new ConflictException(ErrorCode.APPLICATION_POSITION_CLOSED.getCode(),
                    ErrorCode.APPLICATION_POSITION_CLOSED.getDefaultMessage());
        }
        validateCreateDates(request, position);
        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getId, request.getResumeId()).eq(Resume::getUserId, userId));
            if (resume == null) {
                throw new NotFoundException("简历不存在");
            }
        }
        if (applicationMapper.selectActiveByPosition(userId, request.getPositionId()) != null) {
            throw new ConflictException(ErrorCode.APPLICATION_ALREADY_EXISTS.getCode(),
                    ErrorCode.APPLICATION_ALREADY_EXISTS.getDefaultMessage());
        }

        JobApplication application = new JobApplication();
        application.setUserId(userId);
        application.setCompanyId(company.getId());
        application.setPositionId(position.getId());
        application.setResumeId(resume == null ? null : resume.getId());
        application.setStatus(ApplicationStatus.SAVED.name());
        application.setPriority(normalize(request.getPriority(), "MEDIUM"));
        application.setSource(trimToNull(request.getSource()));
        application.setAppliedAt(toLocal(request.getAppliedAt()));
        application.setReferralName(trimToNull(request.getReferralName()));
        application.setExpectedSalaryMin(request.getExpectedSalaryMin());
        application.setExpectedSalaryMax(request.getExpectedSalaryMax());
        application.setCurrency(normalize(request.getCurrency(), "CNY"));
        application.setNextAction(trimToNull(request.getNextAction()));
        application.setNextActionAt(toLocal(request.getNextActionAt()));
        application.setNote(trimToNull(request.getNote()));
        application.setArchived(0);
        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException e) {
            throw new ConflictException(ErrorCode.APPLICATION_ALREADY_EXISTS.getCode(),
                    ErrorCode.APPLICATION_ALREADY_EXISTS.getDefaultMessage());
        }

        insertStatusLog(userId, application.getId(), null, ApplicationStatus.SAVED, null);
        auditLogService.recordAfterCommit("APPLICATION_CREATE", userId, "APPLICATION",
                String.valueOf(application.getId()), true, "创建投递", null, null, MDC.get("traceId"));
        return detail(userId, application.getId());
    }

    @Override
    public PageResult<ApplicationVO> page(Long userId, ApplicationQueryRequest request) {
        Page<ApplicationListRow> page = applicationMapper.selectPageRows(
                new Page<>(request.safePage(), request.safePageSize()), userId,
                request.getCompanyId(), request.getPositionId(), trimToNull(request.getStatus()),
                trimToNull(request.getSource()), Boolean.TRUE.equals(request.getArchived()) ? 1 : 0,
                trimToNull(request.getKeyword()), toLocal(request.getAppliedFrom()), toLocal(request.getAppliedTo()),
                toLocal(request.getInterviewFrom()), toLocal(request.getInterviewTo()),
                sortColumn(request.getSortBy()), "asc".equalsIgnoreCase(request.getSortOrder()) ? "ASC" : "DESC");
        return PageResult.of(page.getRecords().stream().map(ApplicationVO::from).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public ApplicationVO detail(Long userId, Long id) {
        return toVO(getOwned(userId, id));
    }

    @Override
    public ApplicationStatus currentStatus(Long userId, Long id) {
        return parseStatus(getOwned(userId, id).getStatus());
    }

    @Override
    @Transactional
    public ApplicationVO update(Long userId, Long id, ApplicationUpdateRequest request) {
        JobApplication current = getOwned(userId, id);
        if (!request.getVersion().equals(current.getVersion())) {
            throw versionConflict();
        }
        BigDecimal effectiveMin = request.getExpectedSalaryMin() == null
                ? current.getExpectedSalaryMin() : request.getExpectedSalaryMin();
        BigDecimal effectiveMax = request.getExpectedSalaryMax() == null
                ? current.getExpectedSalaryMax() : request.getExpectedSalaryMax();
        if (effectiveMin != null && effectiveMax != null && effectiveMin.compareTo(effectiveMax) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "期望薪资下限不能大于上限");
        }
        if (request.getResumeId() != null && !request.getResumeId().equals(current.getResumeId())) {
            if (ApplicationStatus.valueOf(current.getStatus()).ordinal() > ApplicationStatus.ASSESSMENT.ordinal()) {
                throw new ConflictException(ErrorCode.APPLICATION_RESUME_CHANGE_NOT_ALLOWED.getCode(),
                        ErrorCode.APPLICATION_RESUME_CHANGE_NOT_ALLOWED.getDefaultMessage());
            }
            assertResumeOwned(userId, request.getResumeId());
        }
        LocalDateTime appliedAt = request.getAppliedAt() == null ? current.getAppliedAt() : toLocal(request.getAppliedAt());
        LocalDateTime nextActionAt = request.getNextActionAt() == null
                ? current.getNextActionAt() : toLocal(request.getNextActionAt());
        if (nextActionAt != null && appliedAt != null && nextActionAt.isBefore(appliedAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "下一步时间不能早于投递时间");
        }
        int affected = applicationMapper.updateEditable(userId, id, request.getVersion(), request.getResumeId(),
                trimToNull(request.getSource()), appliedAt, trimToNull(request.getReferralName()), request.getExpectedSalaryMin(),
                request.getExpectedSalaryMax(), trimToNull(request.getCurrency()), trimToNull(request.getNextAction()),
                nextActionAt, trimToNull(request.getNote()));
        if (affected == 0) {
            throw updateConflictOrNotFound(userId, id);
        }
        auditLogService.recordAfterCommit("APPLICATION_UPDATE", userId, "APPLICATION", String.valueOf(id), true,
                "修改投递信息", null, null, MDC.get("traceId"));
        return detail(userId, id);
    }

    @Override
    @Transactional
    public ApplicationVO transition(Long userId, Long id, ApplicationTransitionRequest request) {
        ApplicationStatus target = parseStatus(request.getTargetStatus());
        String reason = trimToNull(request.getReason());
        String hash = requestHash(target, reason);
        ApplicationIdempotencyRecord existing = idempotencyMapper.selectByBusinessKey(userId, id, request.getIdempotencyKey());
        if (existing != null) {
            return handleExistingIdempotency(userId, id, existing, hash);
        }

        JobApplication current = getOwned(userId, id);
        ApplicationStatus from = parseStatus(current.getStatus());
        ApplicationTransitionRule rule = stateMachine.requireAllowed(from, target, reason);
        if (!request.getExpectedVersion().equals(current.getVersion())) {
            throw versionConflict();
        }

        int inserted = idempotencyMapper.insertIgnore(userId, id, request.getIdempotencyKey(), hash,
                target.name(), reason);
        if (inserted == 0) {
            ApplicationIdempotencyRecord raced = idempotencyMapper.selectByBusinessKey(userId, id, request.getIdempotencyKey());
            if (raced == null) {
                throw new ConflictException(ErrorCode.APPLICATION_IDEMPOTENCY_IN_PROGRESS.getCode(),
                        ErrorCode.APPLICATION_IDEMPOTENCY_IN_PROGRESS.getDefaultMessage());
            }
            return handleExistingIdempotency(userId, id, raced, hash);
        }

        int affected;
        try {
            affected = applicationMapper.updateStatusWithVersion(userId, id, target.name(), request.getExpectedVersion());
        } catch (PessimisticLockingFailureException e) {
            // MySQL 在两个相同 version 的更新竞争唯一索引/记录锁时可能选择死锁回滚；
            // 对 API 来说它与版本已变化具有相同语义，不能泄露为 500。
            throw versionConflict();
        }
        if (affected == 0) {
            throw updateConflictOrNotFound(userId, id);
        }
        insertStatusLog(userId, id, from, target, reason);
        ApplicationIdempotencyRecord saved = idempotencyMapper.selectByBusinessKey(userId, id, request.getIdempotencyKey());
        if (saved == null || idempotencyMapper.markSucceeded(saved.getId(), request.getExpectedVersion() + 1, target.name()) == 0) {
            throw new IllegalStateException("幂等记录更新失败");
        }
        eventPublisher.publishEvent(new ApplicationStatusChangedEvent(userId, id, from, target, reason, MDC.get("traceId")));
        // rule 在这里显式参与编排，保证规则元数据不会退化成散落判断。
        if (!rule.triggersBusinessEvent()) {
            throw new IllegalStateException("状态规则未声明业务事件");
        }
        return detail(userId, id);
    }

    @Override
    public List<ApplicationTimelineVO> timeline(Long userId, Long id) {
        getOwned(userId, id);
        return statusLogMapper.selectTimeline(userId, id).stream().map(ApplicationTimelineVO::from).toList();
    }

    @Override
    public List<ApplicationStatus> allowedTargets(Long userId, Long id) {
        return stateMachine.allowedTargets(parseStatus(getOwned(userId, id).getStatus()));
    }

    @Override
    @Transactional
    public ApplicationVO archive(Long userId, Long id) {
        JobApplication current = getOwned(userId, id);
        if (current.getArchived() != null && current.getArchived() == 1) {
            return detail(userId, id);
        }
        if (applicationMapper.updateArchived(userId, id, current.getVersion(), 1) == 0) {
            throw updateConflictOrNotFound(userId, id);
        }
        auditLogService.recordAfterCommit("APPLICATION_ARCHIVE", userId, "APPLICATION", String.valueOf(id), true,
                "归档投递", null, null, MDC.get("traceId"));
        return detail(userId, id);
    }

    @Override
    @Transactional
    public ApplicationVO restore(Long userId, Long id) {
        JobApplication current = getOwned(userId, id);
        if (current.getArchived() == null || current.getArchived() == 0) {
            return detail(userId, id);
        }
        if (applicationMapper.updateArchived(userId, id, current.getVersion(), 0) == 0) {
            throw updateConflictOrNotFound(userId, id);
        }
        auditLogService.recordAfterCommit("APPLICATION_RESTORE", userId, "APPLICATION", String.valueOf(id), true,
                "恢复投递", null, null, MDC.get("traceId"));
        return detail(userId, id);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        JobApplication current = getOwned(userId, id);
        if (!ApplicationStatus.SAVED.name().equals(current.getStatus())) {
            throw new ConflictException(ErrorCode.APPLICATION_DELETE_NOT_ALLOWED.getCode(),
                    ErrorCode.APPLICATION_DELETE_NOT_ALLOWED.getDefaultMessage());
        }
        if (applicationMapper.softDeleteSaved(userId, id, current.getVersion()) == 0) {
            throw updateConflictOrNotFound(userId, id);
        }
        auditLogService.recordAfterCommit("APPLICATION_DELETE", userId, "APPLICATION", String.valueOf(id), true,
                "删除草稿投递（保留状态历史）", null, null, MDC.get("traceId"));
    }

    private ApplicationVO handleExistingIdempotency(Long userId, Long id,
                                                     ApplicationIdempotencyRecord existing, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new ConflictException(ErrorCode.APPLICATION_IDEMPOTENCY_KEY_CONFLICT.getCode(),
                    ErrorCode.APPLICATION_IDEMPOTENCY_KEY_CONFLICT.getDefaultMessage());
        }
        if (!"SUCCEEDED".equals(existing.getResultStatus())) {
            throw new ConflictException(ErrorCode.APPLICATION_IDEMPOTENCY_IN_PROGRESS.getCode(),
                    ErrorCode.APPLICATION_IDEMPOTENCY_IN_PROGRESS.getDefaultMessage());
        }
        return detail(userId, id);
    }

    private JobApplication getOwned(Long userId, Long id) {
        JobApplication application = applicationMapper.selectOwned(userId, id);
        if (application == null) {
            throw new NotFoundException("投递记录不存在");
        }
        return application;
    }

    private ApplicationVO toVO(JobApplication application) {
        ApplicationListRow row = new ApplicationListRow();
        row.setId(application.getId());
        row.setCompanyId(application.getCompanyId());
        row.setPositionId(application.getPositionId());
        row.setResumeId(application.getResumeId());
        row.setStatus(application.getStatus());
        row.setPriority(application.getPriority());
        row.setSource(application.getSource());
        row.setAppliedAt(application.getAppliedAt());
        row.setReferralName(application.getReferralName());
        row.setNextAction(application.getNextAction());
        row.setNextActionAt(application.getNextActionAt());
        row.setNote(application.getNote());
        row.setArchived(application.getArchived());
        row.setCreatedAt(application.getCreatedAt());
        row.setUpdatedAt(application.getUpdatedAt());
        row.setVersion(application.getVersion());
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .select(Company::getId, Company::getName).eq(Company::getId, application.getCompanyId())
                .eq(Company::getUserId, application.getUserId()));
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .select(Position::getId, Position::getTitle).eq(Position::getId, application.getPositionId())
                .eq(Position::getUserId, application.getUserId()));
        if (company != null) row.setCompanyName(company.getName());
        if (position != null) row.setPositionTitle(position.getTitle());
        if (application.getResumeId() != null) {
            Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                    .select(Resume::getId, Resume::getVersionName).eq(Resume::getId, application.getResumeId())
                    .eq(Resume::getUserId, application.getUserId()));
            if (resume != null) row.setResumeVersionName(resume.getVersionName());
        }
        return ApplicationVO.from(row);
    }

    private void insertStatusLog(Long userId, Long id, ApplicationStatus from, ApplicationStatus to, String reason) {
        ApplicationStatusLog log = new ApplicationStatusLog();
        log.setUserId(userId);
        log.setOperatorUserId(userId);
        log.setApplicationId(id);
        log.setFromStatus(from == null ? null : from.name());
        log.setToStatus(to.name());
        log.setChangeNote(reason);
        log.setChangedAt(LocalDateTime.now());
        log.setTraceId(MDC.get("traceId"));
        statusLogMapper.insert(log);
    }

    private void validateCreateDates(ApplicationCreateRequest request, Position position) {
        BigDecimal min = request.getExpectedSalaryMin();
        BigDecimal max = request.getExpectedSalaryMax();
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "期望薪资下限不能大于上限");
        }
        LocalDateTime appliedAt = toLocal(request.getAppliedAt());
        LocalDateTime nextActionAt = toLocal(request.getNextActionAt());
        if (appliedAt != null && position.getDeadlineAt() != null && appliedAt.isAfter(position.getDeadlineAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "投递时间不能晚于岗位截止时间");
        }
        if (appliedAt != null && nextActionAt != null && nextActionAt.isBefore(appliedAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "下一步时间不能早于投递时间");
        }
    }

    private void assertResumeOwned(Long userId, Long resumeId) {
        if (resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId)) == null) {
            throw new NotFoundException("简历不存在");
        }
    }

    private ApplicationStatus parseStatus(String value) {
        try {
            return ApplicationStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.APPLICATION_INVALID_STATUS_TRANSITION.getCode(), "状态不合法");
        }
    }

    private String requestHash(ApplicationStatus target, String reason) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((target.name() + "\n" + (reason == null ? "" : reason)).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private BusinessException updateConflictOrNotFound(Long userId, Long id) {
        return applicationMapper.selectOwned(userId, id) == null
                ? new NotFoundException("投递记录不存在")
                : versionConflict();
    }

    private ConflictException versionConflict() {
        return new ConflictException(ErrorCode.APPLICATION_VERSION_CONFLICT.getCode(),
                ErrorCode.APPLICATION_VERSION_CONFLICT.getDefaultMessage());
    }

    private String sortColumn(String sortBy) {
        return switch (sortBy == null ? "updatedAt" : sortBy) {
            case "createdAt" -> "a.created_at";
            case "appliedAt" -> "a.applied_at";
            case "nextActionAt" -> "a.next_action_at";
            case "status" -> "a.status";
            default -> "a.updated_at";
        };
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(ZONE).toLocalDateTime();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }
}
