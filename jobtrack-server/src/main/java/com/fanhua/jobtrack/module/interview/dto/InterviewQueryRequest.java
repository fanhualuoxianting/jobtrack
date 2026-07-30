package com.fanhua.jobtrack.module.interview.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Data
public class InterviewQueryRequest {
    private String status;
    private Long companyId;
    private Long applicationId;
    private OffsetDateTime from;
    private OffsetDateTime to;
    private Integer page = 1;
    private Integer pageSize = 20;

    public int safePage() { return page == null || page < 1 ? 1 : Math.min(page, 10000); }
    public int safePageSize() { return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100); }
    public String safeStatus() { return StringUtils.hasText(status) ? status.trim() : null; }
}
