package com.fanhua.jobtrack.module.application.domain;

/** 状态流转成功事件；监听器只在事务提交后处理成功副作用。 */
public record ApplicationStatusChangedEvent(
        Long userId,
        Long applicationId,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String reason,
        String traceId) {
}
