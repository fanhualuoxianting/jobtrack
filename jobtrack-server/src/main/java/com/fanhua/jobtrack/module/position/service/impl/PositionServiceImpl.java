package com.fanhua.jobtrack.module.position.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.company.entity.Company;
import com.fanhua.jobtrack.module.company.mapper.CompanyMapper;
import com.fanhua.jobtrack.module.position.dto.PositionQueryRequest;
import com.fanhua.jobtrack.module.position.dto.PositionStatusRequest;
import com.fanhua.jobtrack.module.position.dto.PositionUpsertRequest;
import com.fanhua.jobtrack.module.position.entity.Position;
import com.fanhua.jobtrack.module.position.mapper.PositionMapper;
import com.fanhua.jobtrack.module.position.service.PositionService;
import com.fanhua.jobtrack.module.position.vo.PositionDetailVO;
import com.fanhua.jobtrack.module.position.vo.PositionListVO;
import com.fanhua.jobtrack.module.position.vo.PositionOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位服务实现。
 * 归属校验全部在 SQL 层：公司归属、岗位归属、删除条件都带 user_id。
 */
@Slf4j
@Service
public class PositionServiceImpl implements PositionService {

    private final PositionMapper positionMapper;
    private final CompanyMapper companyMapper;
    private final AuditLogService auditLogService;

    public PositionServiceImpl(PositionMapper positionMapper,
                               CompanyMapper companyMapper,
                               AuditLogService auditLogService) {
        this.positionMapper = positionMapper;
        this.companyMapper = companyMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResult<PositionListVO> pagePositions(Long userId, PositionQueryRequest query) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<Position>()
                .eq(Position::getUserId, userId)
                .eq(query.getCompanyId() != null, Position::getCompanyId, query.getCompanyId())
                .eq(notBlank(query.getCity()), Position::getCity, query.getCity())
                .eq(notBlank(query.getWorkType()), Position::getWorkType, query.getWorkType())
                .eq(notBlank(query.getWorkplaceType()), Position::getWorkplaceType, query.getWorkplaceType())
                .eq(notBlank(query.getStatus()), Position::getStatus, query.getStatus())
                .eq(notBlank(query.getSource()), Position::getSource, query.getSource())
                .ge(query.getSalaryMin() != null, Position::getSalaryMin, query.getSalaryMin())
                .le(query.getSalaryMax() != null, Position::getSalaryMax, query.getSalaryMax())
                .ge(query.getDeadlineFrom() != null, Position::getDeadlineAt, toLocal(query.getDeadlineFrom()))
                .le(query.getDeadlineTo() != null, Position::getDeadlineAt, toLocal(query.getDeadlineTo()));
        if (notBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Position::getTitle, keyword)
                    .or().like(Position::getDepartment, keyword)
                    .or().like(Position::getRequirements, keyword));
        }
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        switch (query.getSortBy()) {
            case "createdAt" -> wrapper.orderBy(true, asc, Position::getCreatedAt);
            case "deadlineAt" -> wrapper.orderBy(true, asc, Position::getDeadlineAt);
            case "salaryMax" -> wrapper.orderBy(true, asc, Position::getSalaryMax);
            default -> wrapper.orderBy(true, asc, Position::getUpdatedAt);
        }

        Page<Position> page = positionMapper.selectPage(new Page<>(query.safePage(), query.safePageSize()), wrapper);
        Map<Long, String> companyNames = loadCompanyNames(userId,
                page.getRecords().stream().map(Position::getCompanyId).distinct().toList());

        List<PositionListVO> vos = page.getRecords().stream()
                .map(p -> PositionListVO.from(p, companyNames.getOrDefault(p.getCompanyId(), null)))
                .toList();
        return PageResult.of(vos, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public PositionDetailVO getDetail(Long userId, Long id) {
        Position position = getOwnedPosition(userId, id);
        return PositionDetailVO.from(position, loadCompanyName(userId, position.getCompanyId()));
    }

    @Override
    @Transactional
    public PositionDetailVO create(Long userId, PositionUpsertRequest request) {
        assertCompanyOwned(userId, request.getCompanyId());
        validateBusinessRules(request);
        assertNoDuplicate(userId, request, null);

        Position position = new Position();
        position.setUserId(userId);
        applyUpsert(position, request);
        position.setStatus("OPEN");
        positionMapper.insert(position);

        audit("POSITION_CREATE", userId, position.getId(), "创建岗位");
        return PositionDetailVO.from(position, loadCompanyName(userId, position.getCompanyId()));
    }

    @Override
    @Transactional
    public PositionDetailVO update(Long userId, Long id, PositionUpsertRequest request) {
        Position position = getOwnedPosition(userId, id);
        assertCompanyOwned(userId, request.getCompanyId());
        validateBusinessRules(request);
        assertNoDuplicate(userId, request, id);

        applyUpsert(position, request);
        // UPDATE 语句本身携带 id + user_id，乐观锁拦截器附加 version 条件
        int affected = positionMapper.update(position, new LambdaQueryWrapper<Position>()
                .eq(Position::getId, id)
                .eq(Position::getUserId, userId));
        if (affected == 0) {
            throw new ConflictException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
                    "记录已被其他请求修改，请刷新后重试");
        }
        audit("POSITION_UPDATE", userId, id, "修改岗位");
        return PositionDetailVO.from(position, loadCompanyName(userId, position.getCompanyId()));
    }

    @Override
    @Transactional
    public PositionDetailVO changeStatus(Long userId, Long id, PositionStatusRequest request) {
        Position position = getOwnedPosition(userId, id);
        position.setStatus(request.getStatus());
        // 状态变更同样走 id + user_id 条件 UPDATE
        int affected = positionMapper.update(position, new LambdaQueryWrapper<Position>()
                .eq(Position::getId, id)
                .eq(Position::getUserId, userId));
        if (affected == 0) {
            throw new ConflictException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
                    "记录已被其他请求修改，请刷新后重试");
        }
        audit("POSITION_UPDATE", userId, id, "岗位状态调整为 " + request.getStatus());
        return PositionDetailVO.from(position, loadCompanyName(userId, position.getCompanyId()));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        getOwnedPosition(userId, id);
        if (positionMapper.countApplicationsOfPosition(userId, id) > 0) {
            throw new ConflictException(ErrorCode.POSITION_HAS_APPLICATIONS.getCode(),
                    "岗位已存在投递记录，可将状态改为 CLOSED 而不是删除");
        }
        int affected = positionMapper.delete(new LambdaQueryWrapper<Position>()
                .eq(Position::getId, id)
                .eq(Position::getUserId, userId));
        if (affected == 0) {
            throw new NotFoundException("岗位不存在");
        }
        audit("POSITION_DELETE", userId, id, "删除岗位");
    }

    @Override
    public List<PositionOptionVO> listOptions(Long userId, Long companyId) {
        List<Position> positions = positionMapper.selectList(new LambdaQueryWrapper<Position>()
                .select(Position::getId, Position::getCompanyId, Position::getTitle, Position::getCity)
                .eq(Position::getUserId, userId)
                .eq(Position::getStatus, "OPEN")
                .eq(companyId != null, Position::getCompanyId, companyId)
                .orderByDesc(Position::getUpdatedAt)
                .last("LIMIT 200"));
        return positions.stream().map(p -> {
            PositionOptionVO vo = new PositionOptionVO();
            vo.setId(p.getId());
            vo.setCompanyId(p.getCompanyId());
            vo.setTitle(p.getTitle());
            vo.setCity(p.getCity());
            return vo;
        }).toList();
    }

    // ---------- 内部辅助 ----------

    /** 岗位归属 + 存在性：查不到统一 404，不给越权者区分"不存在"与"他人的" */
    private Position getOwnedPosition(Long userId, Long id) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getId, id)
                .eq(Position::getUserId, userId));
        if (position == null) {
            throw new NotFoundException("岗位不存在");
        }
        return position;
    }

    /** 公司必须存在且属于当前用户 */
    private void assertCompanyOwned(Long userId, Long companyId) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getId, companyId)
                .eq(Company::getUserId, userId));
        if (company == null) {
            throw new NotFoundException("公司不存在");
        }
    }

    /** 薪资区间、时间与 URL 等业务规则 */
    private void validateBusinessRules(PositionUpsertRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "薪资下限不能大于薪资上限");
        }
        if (request.getPublishedAt() != null && request.getDeadlineAt() != null
                && request.getDeadlineAt().isBefore(request.getPublishedAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), "截止时间不能早于发布时间");
        }
    }

    /** 同公司下 岗位名称+来源 组合重复检查（仅未删除记录；来源为空按 NULL/空串处理） */
    private void assertNoDuplicate(Long userId, PositionUpsertRequest request, Long excludeId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<Position>()
                .eq(Position::getUserId, userId)
                .eq(Position::getCompanyId, request.getCompanyId())
                .eq(Position::getTitle, request.getTitle().trim())
                .ne(excludeId != null, Position::getId, excludeId);
        String normSource = trimToNull(request.getSource());
        if (normSource == null) {
            wrapper.and(w -> w.isNull(Position::getSource).or().eq(Position::getSource, ""));
        } else {
            wrapper.eq(Position::getSource, normSource);
        }
        if (positionMapper.exists(wrapper)) {
            throw new ConflictException(ErrorCode.POSITION_DUPLICATE.getCode(),
                    ErrorCode.POSITION_DUPLICATE.getDefaultMessage());
        }
    }

    private Map<Long, String> loadCompanyNames(Long userId, List<Long> companyIds) {
        Map<Long, String> names = new HashMap<>();
        if (companyIds.isEmpty()) {
            return names;
        }
        List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
                .select(Company::getId, Company::getName)
                .eq(Company::getUserId, userId)
                .in(Company::getId, companyIds));
        for (Company c : companies) {
            names.put(c.getId(), c.getName());
        }
        return names;
    }

    private String loadCompanyName(Long userId, Long companyId) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .select(Company::getId, Company::getName)
                .eq(Company::getId, companyId)
                .eq(Company::getUserId, userId));
        return company == null ? null : company.getName();
    }

    private void applyUpsert(Position position, PositionUpsertRequest request) {
        position.setCompanyId(request.getCompanyId());
        position.setTitle(request.getTitle().trim());
        position.setDepartment(trimToNull(request.getDepartment()));
        position.setCity(trimToNull(request.getCity()));
        position.setWorkType(request.getWorkType());
        position.setWorkplaceType(request.getWorkplaceType());
        position.setSalaryMin(request.getSalaryMin());
        position.setSalaryMax(request.getSalaryMax());
        position.setSalaryUnit(trimToNull(request.getSalaryUnit()));
        position.setCurrency(notBlank(request.getCurrency()) ? request.getCurrency().trim() : "CNY");
        position.setSource(trimToNull(request.getSource()));
        position.setSourceUrl(trimToNull(request.getSourceUrl()));
        position.setDescription(trimToNull(request.getDescription()));
        position.setRequirements(trimToNull(request.getRequirements()));
        position.setPublishedAt(toLocal(request.getPublishedAt()));
        position.setDeadlineAt(toLocal(request.getDeadlineAt()));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** OffsetDateTime 转数据库 LocalDateTime（按 Asia/Shanghai） */
    private LocalDateTime toLocal(java.time.OffsetDateTime offset) {
        return offset == null ? null
                : offset.atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
    }

    private void audit(String action, Long userId, Long positionId, String detail) {
        // 成功类审计：事务提交后才写入
        auditLogService.recordAfterCommit(action, userId, "POSITION", String.valueOf(positionId), true, detail,
                null, null, org.slf4j.MDC.get("traceId"));
    }
}
