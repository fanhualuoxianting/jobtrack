package com.fanhua.jobtrack.module.reminder.domain;

/**
 * 提醒状态是数据库事实状态，不等同于 SSE 在线连接状态。
 * PENDING=待扫描；READY=数据库原子抢占成功、正在处理；
 * SENT=通知已持久化，用户在线与否不影响成功；FAILED=本次处理失败且已到最大重试次数；
 * CANCELLED=业务取消后不再发送。
 */
public enum ReminderStatus {
    PENDING, READY, SENT, FAILED, CANCELLED
}
