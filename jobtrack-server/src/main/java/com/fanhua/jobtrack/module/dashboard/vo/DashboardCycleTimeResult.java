package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.Data;

@Data
public class DashboardCycleTimeResult {
    private DashboardCycleTimeVO appliedToInterviewing;
    private DashboardCycleTimeVO appliedToOffered;
    private DashboardCycleTimeVO appliedToTerminal;
}
