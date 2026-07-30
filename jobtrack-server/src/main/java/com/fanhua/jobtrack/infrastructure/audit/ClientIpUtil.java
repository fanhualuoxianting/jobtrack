package com.fanhua.jobtrack.infrastructure.audit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端信息提取工具（IP / User-Agent）
 */
public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    /** 取客户端真实 IP：优先 X-Forwarded-For 第一个地址 */
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        return ua.length() <= 500 ? ua : ua.substring(0, 500);
    }
}
