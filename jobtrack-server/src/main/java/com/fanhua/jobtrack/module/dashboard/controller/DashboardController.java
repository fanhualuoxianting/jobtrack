package com.fanhua.jobtrack.module.dashboard.controller;

import com.fanhua.jobtrack.common.api.Result;
import com.fanhua.jobtrack.module.dashboard.dto.DashboardQuery;
import com.fanhua.jobtrack.module.dashboard.service.DashboardService;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardBreakdownItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardCycleTimeResult;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardFunnelItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardSummaryVO;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardTrendItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardUpcomingVO;
import com.fanhua.jobtrack.security.SecurityUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.summary(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/funnel")
    public Result<List<DashboardFunnelItem>> funnel(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.funnel(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/trends")
    public Result<List<DashboardTrendItem>> trends(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.trends(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/sources")
    public Result<List<DashboardBreakdownItem>> sources(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.sources(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/industries")
    public Result<List<DashboardBreakdownItem>> industries(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.industries(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/cycle-time")
    public Result<DashboardCycleTimeResult> cycleTime(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.cycleTime(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/upcoming")
    public Result<List<DashboardUpcomingVO>> upcoming(@ParameterObject DashboardQuery query) {
        return Result.success(dashboardService.upcoming(SecurityUtils.currentUserId(), query));
    }
}
