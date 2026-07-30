package com.fanhua.jobtrack.module.dashboard.service.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.common.enums.ErrorCode;
import com.fanhua.jobtrack.common.exception.BusinessException;
import com.fanhua.jobtrack.common.exception.NotFoundException;
import com.fanhua.jobtrack.module.company.entity.Company;
import com.fanhua.jobtrack.module.company.mapper.CompanyMapper;
import com.fanhua.jobtrack.module.dashboard.dto.DashboardQuery;
import com.fanhua.jobtrack.module.dashboard.mapper.DashboardCycleRow;
import com.fanhua.jobtrack.module.dashboard.mapper.DashboardFunnelRow;
import com.fanhua.jobtrack.module.dashboard.mapper.DashboardMapper;
import com.fanhua.jobtrack.module.dashboard.mapper.DashboardSummaryRow;
import com.fanhua.jobtrack.module.dashboard.mapper.DashboardTrendRow;
import com.fanhua.jobtrack.module.dashboard.service.DashboardCacheService;
import com.fanhua.jobtrack.module.dashboard.service.DashboardService;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardCycleTimeResult;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardCycleTimeVO;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardBreakdownItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardFunnelItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardSummaryVO;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardTrendItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardUpcomingVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DashboardMapper dashboardMapper;
    private final CompanyMapper companyMapper;
    private final DashboardCacheService cache;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper, CompanyMapper companyMapper,
                                DashboardCacheService cache, ObjectMapper objectMapper) {
        this.dashboardMapper = dashboardMapper;
        this.companyMapper = companyMapper;
        this.cache = cache;
        this.objectMapper = objectMapper;
    }

    @Override
    public DashboardSummaryVO summary(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        return cached("summary", c, DashboardSummaryVO.class, () -> toSummary(dashboardMapper.selectSummary(
                userId, c.appFrom, c.appTo, c.appNow, c.upcomingFrom, c.upcomingTo,
                c.companyId, c.source, c.includeArchived)));
    }

    @Override
    public List<DashboardFunnelItem> funnel(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, DashboardFunnelItem.class);
        return cached("funnel", c, type, () -> {
            DashboardFunnelRow row = dashboardMapper.selectFunnel(userId, c.appFrom, c.appTo,
                    c.companyId, c.source, c.includeArchived);
            return List.of(
                    new DashboardFunnelItem("APPLIED", "已投递", value(row.getAppliedCount())),
                    new DashboardFunnelItem("ASSESSMENT", "测评", value(row.getAssessmentCount())),
                    new DashboardFunnelItem("INTERVIEWING", "面试", value(row.getInterviewCount())),
                    new DashboardFunnelItem("OFFERED", "Offer", value(row.getOfferedCount())),
                    new DashboardFunnelItem("ACCEPTED", "接受 Offer", value(row.getAcceptedCount())));
        });
    }

    @Override
    public List<DashboardTrendItem> trends(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, DashboardTrendItem.class);
        return cached("trends", c, type, () -> {
            List<DashboardTrendRow> rows = dashboardMapper.selectTrends(userId, c.appFrom, c.appTo,
                    c.timezone, c.bucketExpression, c.companyId, c.source, c.includeArchived);
            Map<String, Long> values = new HashMap<>();
            rows.forEach(row -> values.put(row.getBucket(), value(row.getCount())));
            List<DashboardTrendItem> result = new ArrayList<>();
            for (String bucket : c.buckets()) result.add(new DashboardTrendItem(bucket, values.getOrDefault(bucket, 0L)));
            return result;
        });
    }

    @Override
    public List<DashboardBreakdownItem> sources(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, DashboardBreakdownItem.class);
        return cached("sources", c, type, () -> dashboardMapper.selectSources(userId, c.appFrom, c.appTo,
                c.companyId, c.includeArchived).stream().map(item -> new DashboardBreakdownItem(item.getName(), item.getCount())).toList());
    }

    @Override
    public List<DashboardBreakdownItem> industries(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, DashboardBreakdownItem.class);
        return cached("industries", c, type, () -> dashboardMapper.selectIndustries(userId, c.appFrom, c.appTo,
                c.companyId, c.includeArchived).stream().map(item -> new DashboardBreakdownItem(item.getName(), item.getCount())).toList());
    }

    @Override
    public DashboardCycleTimeResult cycleTime(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        return cached("cycle-time", c, DashboardCycleTimeResult.class, () -> {
            DashboardCycleTimeResult result = new DashboardCycleTimeResult();
            for (DashboardCycleRow row : dashboardMapper.selectCycleTimes(userId, c.appFrom, c.appTo,
                    c.companyId, c.source, c.includeArchived)) {
                DashboardCycleTimeVO value = new DashboardCycleTimeVO();
                value.setSampleCount(value(row.getSampleCount()));
                value.setAverageHours(row.getAverageHours() == null ? BigDecimal.ZERO : row.getAverageHours().setScale(2, RoundingMode.HALF_UP));
                value.setMinHours(row.getMinHours() == null ? BigDecimal.ZERO : row.getMinHours().setScale(2, RoundingMode.HALF_UP));
                value.setMaxHours(row.getMaxHours() == null ? BigDecimal.ZERO : row.getMaxHours().setScale(2, RoundingMode.HALF_UP));
                switch (row.getMetricType()) {
                    case "APPLIED_TO_INTERVIEWING" -> result.setAppliedToInterviewing(value);
                    case "APPLIED_TO_OFFERED" -> result.setAppliedToOffered(value);
                    case "APPLIED_TO_TERMINAL" -> result.setAppliedToTerminal(value);
                    default -> { }
                }
            }
            if (result.getAppliedToInterviewing() == null) result.setAppliedToInterviewing(emptyCycle());
            if (result.getAppliedToOffered() == null) result.setAppliedToOffered(emptyCycle());
            if (result.getAppliedToTerminal() == null) result.setAppliedToTerminal(emptyCycle());
            return result;
        });
    }

    @Override
    public List<DashboardUpcomingVO> upcoming(Long userId, DashboardQuery query) {
        Context c = context(userId, query);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, DashboardUpcomingVO.class);
        return cached("upcoming", c, type, () -> dashboardMapper.selectUpcoming(userId, c.upcomingFrom, c.upcomingTo,
                c.companyId, c.source, c.includeArchived).stream().map(item -> {
            if (item.getScheduledStartAt() != null) {
                LocalDateTime utc = LocalDateTime.parse(item.getScheduledStartAt().toString().replace(' ', 'T'));
                item.setScheduledStartAt(utc.atOffset(UTC).atZoneSameInstant(c.zone).toOffsetDateTime().toString());
            }
            return item;
        }).toList());
    }

    private DashboardSummaryVO toSummary(DashboardSummaryRow row) {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setTotalApplications(value(row.getTotalApplications()));
        vo.setInProgressApplications(value(row.getInProgressApplications()));
        vo.setPendingTasks(value(row.getPendingTasks()));
        vo.setUpcomingInterviews(value(row.getUpcomingInterviews()));
        vo.setOfferApplications(value(row.getOfferApplications()));
        vo.setAcceptedOffers(value(row.getAcceptedOffers()));
        vo.setAdvancedApplications(value(row.getAdvancedApplications()));
        vo.setInterviewApplications(value(row.getInterviewApplications()));
        vo.setOfferedApplications(value(row.getOfferedApplications()));
        vo.setOverdueReminders(value(row.getOverdueReminders()));
        vo.setInterviewConversionRate(rate(vo.getInterviewApplications(), vo.getAdvancedApplications()));
        vo.setOfferConversionRate(rate(vo.getOfferedApplications(), vo.getAdvancedApplications()));
        return vo;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private DashboardCycleTimeVO emptyCycle() {
        DashboardCycleTimeVO value = new DashboardCycleTimeVO();
        value.setAverageHours(BigDecimal.ZERO.setScale(2)); value.setMinHours(BigDecimal.ZERO.setScale(2)); value.setMaxHours(BigDecimal.ZERO.setScale(2));
        return value;
    }

    private long value(Number value) { return value == null ? 0 : value.longValue(); }

    private <T> T cached(String type, Context context, Class<T> valueType, Supplier<T> loader) {
        return cache.getOrLoad(cache.key(context.userId, type, context.filter), objectMapper.getTypeFactory().constructType(valueType), loader);
    }

    private <T> T cached(String type, Context context, JavaType valueType, Supplier<T> loader) {
        return cache.getOrLoad(cache.key(context.userId, type, context.filter), valueType, loader);
    }

    private Context context(Long userId, DashboardQuery query) {
        DashboardQuery input = query == null ? new DashboardQuery() : query;
        ZoneId zone = zone(input.getTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate start = input.getStartDate() == null ? today.minusDays(29) : input.getStartDate();
        LocalDate end = input.getEndDate() == null ? today : input.getEndDate();
        if (start.isAfter(end)) throw validation("startDate 不能晚于 endDate");
        if (end.toEpochDay() - start.toEpochDay() > 366) throw validation("统计时间范围不能超过 367 天");
        if (input.getCompanyId() != null && companyMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Company>()
                .select(Company::getId).eq(Company::getId, input.getCompanyId()).eq(Company::getUserId, userId)) == null) {
            throw new NotFoundException("公司不存在");
        }
        boolean includeArchived = Boolean.TRUE.equals(input.getIncludeArchived());
        String granularity = input.getGranularity() == null ? "DAY" : input.getGranularity().trim().toUpperCase();
        if (!List.of("DAY", "WEEK", "MONTH").contains(granularity)) throw validation("granularity 只支持 DAY、WEEK、MONTH");
        LocalDateTime appFrom = start.atStartOfDay(zone).withZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
        LocalDateTime appTo = end.plusDays(1).atStartOfDay(zone).withZoneSameInstant(STORAGE_ZONE).toLocalDateTime();
        LocalDateTime upcomingFrom = LocalDateTime.now(UTC);
        LocalDateTime upcomingTo = upcomingFrom.plusDays(7);
        LocalDateTime selectedUtcFrom = start.atStartOfDay(zone).withZoneSameInstant(UTC).toLocalDateTime();
        LocalDateTime selectedUtcTo = end.plusDays(1).atStartOfDay(zone).withZoneSameInstant(UTC).toLocalDateTime();
        if (selectedUtcFrom.isAfter(upcomingFrom)) upcomingFrom = selectedUtcFrom;
        if (selectedUtcTo.isBefore(upcomingTo)) upcomingTo = selectedUtcTo;
        return new Context(userId, zone, input.getTimezone() == null ? "Asia/Shanghai" : zone.getId(), start, end,
                appFrom, appTo, LocalDateTime.now(STORAGE_ZONE), upcomingFrom, upcomingTo, input.getCompanyId(),
                input.getSource() == null ? null : input.getSource().trim(), includeArchived, granularity,
                bucketExpression(granularity), filter(input, start, end, zone, includeArchived, granularity));
    }

    private ZoneId zone(String value) {
        try { return ZoneId.of(value == null || value.isBlank() ? "Asia/Shanghai" : value.trim()); }
        catch (Exception e) { throw validation("timezone 必须是合法 IANA 时区"); }
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR.getCode(), message);
    }

    private String bucketExpression(String granularity) {
        String instant = "CONVERT_TZ(COALESCE(a.applied_at,a.created_at), '+08:00', #{timezone})";
        return switch (granularity) {
            case "WEEK" -> "DATE_FORMAT(DATE_SUB(" + instant + ", INTERVAL WEEKDAY(" + instant + ") DAY), '%Y-%m-%d')";
            case "MONTH" -> "DATE_FORMAT(" + instant + ", '%Y-%m')";
            default -> "DATE_FORMAT(" + instant + ", '%Y-%m-%d')";
        };
    }

    private String filter(DashboardQuery input, LocalDate start, LocalDate end, ZoneId zone,
                          boolean includeArchived, String granularity) {
        return String.join("|", start.toString(), end.toString(), zone.getId(),
                input.getCompanyId() == null ? "" : input.getCompanyId().toString(),
                input.getSource() == null ? "" : input.getSource().trim(), Boolean.toString(includeArchived), granularity);
    }

    private record Context(Long userId, ZoneId zone, String timezone, LocalDate start, LocalDate end,
                           LocalDateTime appFrom, LocalDateTime appTo, LocalDateTime appNow,
                           LocalDateTime upcomingFrom, LocalDateTime upcomingTo, Long companyId, String source,
                           boolean includeArchived, String granularity, String bucketExpression, String filter) {
        List<String> buckets() {
            List<String> result = new ArrayList<>();
            if ("MONTH".equals(granularity)) {
                LocalDate cursor = start.withDayOfMonth(1);
                LocalDate last = end.withDayOfMonth(1);
                while (!cursor.isAfter(last)) { result.add(cursor.format(MONTH)); cursor = cursor.plusMonths(1); }
            } else {
                LocalDate cursor = "WEEK".equals(granularity) ? start.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) : start;
                LocalDate last = "WEEK".equals(granularity) ? end.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) : end;
                while (!cursor.isAfter(last)) { result.add(cursor.format(DAY)); cursor = cursor.plusWeeks("WEEK".equals(granularity) ? 1 : 0).plusDays("WEEK".equals(granularity) ? 0 : 1); }
            }
            return result;
        }
    }
}
