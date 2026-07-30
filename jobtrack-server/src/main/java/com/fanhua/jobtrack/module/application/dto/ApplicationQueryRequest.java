package com.fanhua.jobtrack.module.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApplicationQueryRequest {
    @Size(max = 100)
    private String keyword;
    private Long companyId;
    private Long positionId;
    @Pattern(regexp = "^$|^(SAVED|APPLIED|ASSESSMENT|INTERVIEWING|OFFERED|ACCEPTED|REJECTED|WITHDRAWN|CLOSED)$")
    private String status;
    @Size(max = 50)
    private String source;
    private Boolean archived = false;
    private OffsetDateTime appliedFrom;
    private OffsetDateTime appliedTo;
    private OffsetDateTime interviewFrom;
    private OffsetDateTime interviewTo;
    private long page = 1;
    private long pageSize = 20;
    @Pattern(regexp = "^(updatedAt|createdAt|appliedAt|nextActionAt|status)$", message = "不支持的排序字段")
    private String sortBy = "updatedAt";
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向只支持 asc/desc")
    private String sortOrder = "desc";

    public long safePage() { return Math.max(page, 1); }
    public long safePageSize() { return pageSize < 1 ? 20 : Math.min(pageSize, 100); }
}
