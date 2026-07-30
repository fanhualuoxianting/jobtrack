package com.fanhua.jobtrack.common.util;

/**
 * 日志脱敏工具：防止完整账号进入日志后被用于撞库
 */
public final class MaskingUtils {

    private MaskingUtils() {
    }

    /** 账号脱敏：邮箱保留首字符和域名，其他保留首尾字符 */
    public static String maskAccount(String account) {
        if (account == null || account.length() <= 2) {
            return "***";
        }
        int at = account.indexOf('@');
        if (at > 1) {
            return account.charAt(0) + "***" + account.substring(at);
        }
        return account.charAt(0) + "***" + account.charAt(account.length() - 1);
    }
}
