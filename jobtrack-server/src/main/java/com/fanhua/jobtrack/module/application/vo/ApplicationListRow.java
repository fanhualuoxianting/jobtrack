package com.fanhua.jobtrack.module.application.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** JOIN 查询中间结果，避免列表循环逐条查询公司、岗位和简历。 */
@Data
public class ApplicationListRow {
    private Long id;
    private Long companyId;
    private String companyName;
    private Long positionId;
    private String positionTitle;
    private Long resumeId;
    private String resumeVersionName;
    private String status;
    private String priority;
    private String source;
    private LocalDateTime appliedAt;
    private String referralName;
    private String nextAction;
    private LocalDateTime nextActionAt;
    private String note;
    private Integer archived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
