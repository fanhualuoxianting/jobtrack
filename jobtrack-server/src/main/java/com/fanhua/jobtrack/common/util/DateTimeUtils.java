package com.fanhua.jobtrack.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 时间转换工具：数据库 DATETIME 无时区，统一按 Asia/Shanghai 解释为带偏移时间输出
 */
public final class DateTimeUtils {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private DateTimeUtils() {
    }

    public static OffsetDateTime toOffset(LocalDateTime local) {
        return local == null ? null : local.atZone(ZONE).toOffsetDateTime();
    }
}
