package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardSummaryVO {
    private long totalApplications;
    private long inProgressApplications;
    private long pendingTasks;
    private long upcomingInterviews;
    private long offerApplications;
    private long acceptedOffers;
    private long advancedApplications;
    private long interviewApplications;
    private long offeredApplications;
    private BigDecimal interviewConversionRate;
    private BigDecimal offerConversionRate;
    private long overdueReminders;
}
