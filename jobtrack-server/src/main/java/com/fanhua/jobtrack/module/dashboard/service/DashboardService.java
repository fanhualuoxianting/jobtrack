package com.fanhua.jobtrack.module.dashboard.service;

import com.fanhua.jobtrack.module.dashboard.dto.DashboardQuery;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardBreakdownItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardCycleTimeResult;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardFunnelItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardSummaryVO;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardTrendItem;
import com.fanhua.jobtrack.module.dashboard.vo.DashboardUpcomingVO;

import java.util.List;

public interface DashboardService {
    DashboardSummaryVO summary(Long userId, DashboardQuery query);
    List<DashboardFunnelItem> funnel(Long userId, DashboardQuery query);
    List<DashboardTrendItem> trends(Long userId, DashboardQuery query);
    List<DashboardBreakdownItem> sources(Long userId, DashboardQuery query);
    List<DashboardBreakdownItem> industries(Long userId, DashboardQuery query);
    DashboardCycleTimeResult cycleTime(Long userId, DashboardQuery query);
    List<DashboardUpcomingVO> upcoming(Long userId, DashboardQuery query);
}
