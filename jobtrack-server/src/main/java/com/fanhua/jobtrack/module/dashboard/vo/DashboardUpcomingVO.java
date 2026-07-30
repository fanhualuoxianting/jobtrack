package com.fanhua.jobtrack.module.dashboard.vo;

import lombok.Data;

@Data
public class DashboardUpcomingVO {
    private Long interviewId;
    private Long applicationId;
    private String companyName;
    private String positionTitle;
    private String roundName;
    private String scheduledStartAt;
    private String timezone;
}
