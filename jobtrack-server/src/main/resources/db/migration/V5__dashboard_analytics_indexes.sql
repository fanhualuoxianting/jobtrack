-- 阶段 7：统计看板聚合查询索引。
-- 不修改历史 Flyway 文件；所有统计 SQL 仍强制携带 user_id、deleted 条件。

ALTER TABLE jt_job_application
    ADD KEY idx_app_dashboard_analytics (user_id, deleted, archived, company_id, source, applied_at, created_at);

ALTER TABLE jt_application_status_log
    ADD KEY idx_status_log_dashboard_analytics (user_id, deleted, to_status, application_id, changed_at);

ALTER TABLE jt_interview
    ADD KEY idx_interview_dashboard_analytics (user_id, deleted, status, scheduled_start, application_id);
