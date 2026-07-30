package com.fanhua.jobtrack.module.dashboard.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DashboardQuery {
    private LocalDate startDate;
    private LocalDate endDate;
    private String timezone;
    private Long companyId;
    private String source;
    private Boolean includeArchived;
    private String granularity;
}
