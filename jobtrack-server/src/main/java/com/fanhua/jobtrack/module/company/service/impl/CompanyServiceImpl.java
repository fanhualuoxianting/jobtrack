package com.fanhua.jobtrack.module.company.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fanhua.jobtrack.common.api.PageResult;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.ConflictException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.infrastructure.audit.AuditAction;
import com.fanhua.jobtrack.infrastructure.audit.AuditLogService;
import com.fanhua.jobtrack.module.dashboard.service.DashboardCacheInvalidation;
import com.fanhua.jobtrack.module.company.dto.CompanyQueryRequest;
import com.fanhua.jobtrack.module.company.dto.CompanyUpsertRequest;
import com.fanhua.jobtrack.module.company.entity.Company;
import com.fanhua.jobtrack.module.company.mapper.CompanyMapper;
import com.fanhua.jobtrack.module.company.service.CompanyService;
import com.fanhua.jobtrack.module.company.vo.CompanyDetailVO;
import com.fanhua.jobtrack.module.company.vo.CompanyListVO;
import com.fanhua.jobtrack.module.company.vo.CompanyOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司服务实现。
 * 数据隔离原则：任何按 ID 的查询/修改/删除都带 user_id 条件，在 SQL 层完成，
 * 不是先查出再在 Java 里判断归属。
 */
@Slf4j
@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final AuditLogService auditLogService;
    private final DashboardCacheInvalidation dashboardCacheInvalidation;

    public CompanyServiceImpl(CompanyMapper companyMapper, AuditLogService auditLogService,
                              DashboardCacheInvalidation dashboardCacheInvalidation) {
        this.companyMapper = companyMapper;
        this.auditLogService = auditLogService;
        this.dashboardCacheInvalidation = dashboardCacheInvalidation;
    }

    @Override
    public PageResult<CompanyListVO> pageCompanies(Long userId, CompanyQueryRequest query) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, userId)
                .eq(query.getCity() != null && !query.getCity().isBlank(), Company::getCity, query.getCity())
                .eq(query.getIndustry() != null && !query.getIndustry().isBlank(), Company::getIndustry, query.getIndustry());
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Company::getName, keyword).or().like(Company::getShortName, keyword));
        }
        // 排序字段由 DTO 层的白名单 Pattern 保证，绝不拼接前端输入
        boolean asc = "asc".equalsIgnoreCase(query.getSortOrder());
        if ("name".equals(query.getSortBy())) {
            wrapper.orderBy(true, asc, Company::getName).orderByDesc(Company::getId);
        } else if ("createdAt".equals(query.getSortBy())) {
            wrapper.orderBy(true, asc, Company::getCreatedAt);
        } else {
            wrapper.orderBy(true, asc, Company::getUpdatedAt);
        }

        Page<Company> page = companyMapper.selectPage(new Page<>(query.safePage(), query.safePageSize()), wrapper);
        List<Company> records = page.getRecords();
        Map<Long, Long> positionCounts = batchCount(userId, records, true);
        Map<Long, Long> applicationCounts = batchCount(userId, records, false);

        List<CompanyListVO> vos = records.stream()
                .map(c -> CompanyListVO.from(c,
                        positionCounts.getOrDefault(c.getId(), 0L),
                        applicationCounts.getOrDefault(c.getId(), 0L)))
                .toList();
        return PageResult.of(vos, page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    public CompanyDetailVO getDetail(Long userId, Long id) {
        return CompanyDetailVO.from(getOwnedCompany(userId, id));
    }

    @Override
    @Transactional
    public CompanyDetailVO create(Long userId, CompanyUpsertRequest request) {
        String normalizedName = normalizeName(request.getName());
        assertNameAvailable(userId, normalizedName, null);

        Company company = new Company();
        company.setUserId(userId);
        applyUpsert(company, request, normalizedName);
        companyMapper.insert(company);
        dashboardCacheInvalidation.afterCommit(userId);

        audit("COMPANY_CREATE", userId, company.getId(), "创建公司");
        return CompanyDetailVO.from(company);
    }

    @Override
    @Transactional
    public CompanyDetailVO update(Long userId, Long id, CompanyUpsertRequest request) {
        Company company = getOwnedCompany(userId, id);
        String normalizedName = normalizeName(request.getName());
        if (!normalizedName.equals(company.getName())) {
            assertNameAvailable(userId, normalizedName, id);
        }
        applyUpsert(company, request, normalizedName);
        // UPDATE 语句本身同时携带 id + user_id（乐观锁拦截器附加 version 条件），
        // 不依赖"先查后写"的信任链；受影响行数为 0 时区分并发冲突与不存在
        int affected = companyMapper.update(company, new LambdaQueryWrapper<Company>()
                .eq(Company::getId, id)
                .eq(Company::getUserId, userId));
        if (affected == 0) {
            throw new ConflictException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
                    "记录已被其他请求修改，请刷新后重试");
        }
        dashboardCacheInvalidation.afterCommit(userId);
        audit("COMPANY_UPDATE", userId, id, "修改公司");
        return CompanyDetailVO.from(company);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        getOwnedCompany(userId, id);
        if (companyMapper.countPositionsOfCompany(userId, id) > 0
                || companyMapper.countApplicationsOfCompany(userId, id) > 0) {
            throw new ConflictException(ErrorCode.COMPANY_HAS_RELATIONS.getCode(),
                    "公司存在关联岗位或投递记录，请先处理");
        }
        // 逻辑删除也带 user_id 条件
        int affected = companyMapper.delete(new LambdaQueryWrapper<Company>()
                .eq(Company::getId, id)
                .eq(Company::getUserId, userId));
        if (affected == 0) {
            throw new NotFoundException("公司不存在");
        }
        dashboardCacheInvalidation.afterCommit(userId);
        audit("COMPANY_DELETE", userId, id, "删除公司");
    }

    @Override
    public List<CompanyOptionVO> listOptions(Long userId) {
        List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
                .select(Company::getId, Company::getName)
                .eq(Company::getUserId, userId)
                .orderByAsc(Company::getName));
        return companies.stream().map(c -> {
            CompanyOptionVO vo = new CompanyOptionVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            return vo;
        }).toList();
    }

    // ---------- 内部辅助 ----------

    /** 按 ID 查询且强制归属当前用户；查不到统一 404（不暴露资源存在性） */
    private Company getOwnedCompany(Long userId, Long id) {
        Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getId, id)
                .eq(Company::getUserId, userId));
        if (company == null) {
            throw new NotFoundException("公司不存在");
        }
        return company;
    }

    /** 名称规范化：去首尾空格，连续空白合一 */
    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    /** 同用户、未删除维度的名称唯一性校验（创建与修改共用） */
    private void assertNameAvailable(Long userId, String normalizedName, Long excludeId) {
        boolean exists = companyMapper.exists(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, userId)
                .eq(Company::getName, normalizedName)
                .ne(excludeId != null, Company::getId, excludeId));
        if (exists) {
            throw new ConflictException(ErrorCode.COMPANY_NAME_EXISTS.getCode(), "公司名称已存在");
        }
    }

    private void applyUpsert(Company company, CompanyUpsertRequest request, String normalizedName) {
        company.setName(normalizedName);
        company.setShortName(trimToNull(request.getShortName()));
        company.setIndustry(trimToNull(request.getIndustry()));
        company.setScale(trimToNull(request.getScale()));
        company.setCity(trimToNull(request.getCity()));
        company.setWebsite(trimToNull(request.getWebsite()));
        company.setDescription(trimToNull(request.getDescription()));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, Long> batchCount(Long userId, List<Company> records, boolean positions) {
        Map<Long, Long> counts = new HashMap<>();
        if (records.isEmpty()) {
            return counts;
        }
        List<Long> ids = records.stream().map(Company::getId).toList();
        List<Map<String, Object>> rows = positions
                ? companyMapper.countPositionsByCompanies(userId, ids)
                : companyMapper.countApplicationsByCompanies(userId, ids);
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("company_id")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        return counts;
    }

    private void audit(String action, Long userId, Long companyId, String detail) {
        // 成功类审计：事务提交后才写入，避免回滚后残留假记录
        auditLogService.recordAfterCommit(action, userId, "COMPANY", String.valueOf(companyId), true, detail,
                null, null, org.slf4j.MDC.get("traceId"));
    }
}
