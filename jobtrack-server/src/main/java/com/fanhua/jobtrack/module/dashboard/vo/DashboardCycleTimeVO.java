package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardCycleTimeVO {
    private long sampleCount;
    private BigDecimal averageHours;
    private BigDecimal minHours;
    private BigDecimal maxHours;
}
