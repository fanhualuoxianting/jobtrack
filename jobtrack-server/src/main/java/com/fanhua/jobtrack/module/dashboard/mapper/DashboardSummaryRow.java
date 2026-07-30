package com.fanhua.jobtrack.module.dashboard.mapper;

import lombok.Data;

@Data
public class DashboardSummaryRow {
    private Long totalApplications;
    private Long inProgressApplications;
    private Long pendingTasks;
    private Long upcomingInterviews;
    private Long offerApplications;
    private Long acceptedOffers;
    private Long advancedApplications;
    private Long interviewApplications;
    private Long offeredApplications;
    private Long overdueReminders;
}
