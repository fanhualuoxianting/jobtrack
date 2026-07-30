package com.fanhua.jobtrack.module.application.domain;

/** 单条状态流转规则，供后端校验和前端按钮展示共同使用。 */
public record ApplicationTransitionRule(
        ApplicationStatus from,
        ApplicationStatus to,
        boolean requiresReason,
        boolean requiresTimeInfo,
        boolean allowsInterview,
        boolean terminal,
        boolean triggersBusinessEvent) {
}
