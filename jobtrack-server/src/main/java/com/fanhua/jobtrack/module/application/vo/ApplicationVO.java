package com.fanhua.jobtrack.module.application.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApplicationVO {
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
    private OffsetDateTime appliedAt;
    private String referralName;
    private String nextAction;
    private OffsetDateTime nextActionAt;
    private String note;
    private Boolean archived;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer version;

    public static ApplicationVO from(ApplicationListRow row) {
        ApplicationVO vo = new ApplicationVO();
        vo.id = row.getId();
        vo.companyId = row.getCompanyId();
        vo.companyName = row.getCompanyName();
        vo.positionId = row.getPositionId();
        vo.positionTitle = row.getPositionTitle();
        vo.resumeId = row.getResumeId();
        vo.resumeVersionName = row.getResumeVersionName();
        vo.status = row.getStatus();
        vo.priority = row.getPriority();
        vo.source = row.getSource();
        vo.appliedAt = DateTimeUtils.toOffset(row.getAppliedAt());
        vo.referralName = row.getReferralName();
        vo.nextAction = row.getNextAction();
        vo.nextActionAt = DateTimeUtils.toOffset(row.getNextActionAt());
        vo.note = row.getNote();
        vo.archived = row.getArchived() != null && row.getArchived() == 1;
        vo.createdAt = DateTimeUtils.toOffset(row.getCreatedAt());
        vo.updatedAt = DateTimeUtils.toOffset(row.getUpdatedAt());
        vo.version = row.getVersion();
        return vo;
    }
}
