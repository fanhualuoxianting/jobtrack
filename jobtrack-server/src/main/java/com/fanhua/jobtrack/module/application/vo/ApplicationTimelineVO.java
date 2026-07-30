package com.fanhua.jobtrack.module.application.vo;

import com.fanhua.jobtrack.common.util.DateTimeUtils;
import com.fanhua.jobtrack.module.application.entity.ApplicationStatusLog;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApplicationTimelineVO {
    private Long id;
    private Long applicationId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Long operatorUserId;
    private OffsetDateTime occurredAt;
    private String traceId;

    public static ApplicationTimelineVO from(ApplicationStatusLog log) {
        ApplicationTimelineVO vo = new ApplicationTimelineVO();
        vo.id = log.getId();
        vo.applicationId = log.getApplicationId();
        vo.fromStatus = log.getFromStatus();
        vo.toStatus = log.getToStatus();
        vo.reason = log.getChangeNote();
        vo.operatorUserId = log.getOperatorUserId() == null ? log.getUserId() : log.getOperatorUserId();
        vo.occurredAt = DateTimeUtils.toOffset(log.getChangedAt());
        vo.traceId = log.getTraceId();
        return vo;
    }
}
