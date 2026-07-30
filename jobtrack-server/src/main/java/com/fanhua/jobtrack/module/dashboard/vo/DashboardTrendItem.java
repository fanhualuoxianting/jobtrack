package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardTrendItem {
    private String bucket;
    private long count;
}
