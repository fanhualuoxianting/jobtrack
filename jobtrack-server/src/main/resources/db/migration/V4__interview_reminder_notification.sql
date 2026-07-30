-- 阶段 6：多轮面试、数据库提醒与站内通知扩展。
-- 不修改历史 Flyway 文件；旧的 round_no/title/note/remind_at 字段继续兼容演示数据。

ALTER TABLE jt_interview
    ADD COLUMN round_name VARCHAR(150) NULL AFTER round_no,
    ADD COLUMN contact_info VARCHAR(500) NULL AFTER interviewer,
    ADD COLUMN feedback TEXT NULL AFTER result,
    ADD COLUMN cancel_reason VARCHAR(500) NULL AFTER feedback,
    ADD COLUMN completed_at DATETIME NULL AFTER cancel_reason;

UPDATE jt_interview
SET round_name = title
WHERE round_name IS NULL;

ALTER TABLE jt_interview
    ADD UNIQUE KEY uk_interview_user_application_round (user_id, application_id, round_no),
    ADD KEY idx_interview_user_time_status (user_id, scheduled_start, status, deleted);

ALTER TABLE jt_reminder
    ADD COLUMN interview_id BIGINT UNSIGNED NULL AFTER user_id,
    ADD COLUMN application_id BIGINT UNSIGNED NULL AFTER interview_id,
    ADD COLUMN reminder_type VARCHAR(40) NULL AFTER application_id,
    ADD COLUMN scheduled_at DATETIME NULL AFTER reminder_type,
    ADD COLUMN cancelled_at DATETIME NULL AFTER read_at,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER fail_reason,
    ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
    ADD COLUMN last_error VARCHAR(500) NULL AFTER max_retries;

UPDATE jt_reminder r
LEFT JOIN jt_interview i ON r.biz_type = 'INTERVIEW' AND r.biz_id = i.id
SET r.interview_id = i.id,
    r.application_id = i.application_id,
    r.reminder_type = COALESCE(r.reminder_type, 'CUSTOM'),
    r.scheduled_at = COALESCE(r.scheduled_at, r.remind_at)
WHERE r.interview_id IS NULL OR r.scheduled_at IS NULL;

ALTER TABLE jt_reminder
    ADD KEY idx_reminder_due (status, scheduled_at, id),
    ADD KEY idx_reminder_user_notification (user_id, status, read_at, created_at),
    ADD KEY idx_reminder_interview (user_id, interview_id, status);
