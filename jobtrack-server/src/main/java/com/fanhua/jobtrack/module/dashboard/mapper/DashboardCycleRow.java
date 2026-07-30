package com.fanhua.jobtrack.module.dashboard.mapper;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardCycleRow {
    private String metricType;
    private Long sampleCount;
    private BigDecimal averageHours;
    private BigDecimal minHours;
    private BigDecimal maxHours;
}
