package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardFunnelItem {
    private String stage;
    private String label;
    private long count;
}
