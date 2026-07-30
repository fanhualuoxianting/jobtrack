package com.fanhua.jobtrack.module.application.domain;

/** 投递状态，状态值与 PROJECT_SPEC 及数据库约定保持一致。 */
public enum ApplicationStatus {
    SAVED, APPLIED, ASSESSMENT, INTERVIEWING, OFFERED, ACCEPTED, REJECTED, WITHDRAWN, CLOSED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == WITHDRAWN || this == CLOSED;
    }
}
