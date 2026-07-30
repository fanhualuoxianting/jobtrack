package com.fanhua.jobtrack.module.reminder.dto;

import lombok.Data;

@Data
public class NotificationQueryRequest {
    private String readState;
    private Integer page = 1;
    private Integer pageSize = 20;

    public int safePage() { return page == null || page < 1 ? 1 : Math.min(page, 10000); }
    public int safePageSize() { return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100); }
    public String safeReadState() {
        return "READ".equalsIgnoreCase(readState) || "UNREAD".equalsIgnoreCase(readState)
                ? readState.toUpperCase() : null;
    }
}
