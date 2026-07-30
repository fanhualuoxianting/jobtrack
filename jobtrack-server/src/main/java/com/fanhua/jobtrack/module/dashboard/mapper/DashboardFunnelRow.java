package com.fanhua.jobtrack.module.dashboard.mapper;

import lombok.Data;

@Data
public class DashboardFunnelRow {
    private Long appliedCount;
    private Long assessmentCount;
    private Long interviewCount;
    private Long offeredCount;
    private Long acceptedCount;
}
