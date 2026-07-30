-- 阶段 5：投递生命周期、状态历史补充字段与数据库幂等。
-- 使用 V3 是因为 dev profile 还扫描 db/demo 下的既有 V2 示例迁移。

ALTER TABLE jt_application_status_log
    ADD COLUMN operator_user_id BIGINT UNSIGNED NULL AFTER user_id,
    ADD COLUMN trace_id VARCHAR(64) NULL AFTER changed_at,
    ADD KEY idx_status_log_operator_time (operator_user_id, changed_at);

-- dev profile 会先加载 db/demo。保留同一用户/岗位最新的一条，其余旧示例投递归档，
-- 这样历史演示数据也能满足新增的“单个活跃投递”唯一约束，不删除任何记录。
UPDATE jt_job_application older
JOIN jt_job_application newer
  ON newer.user_id = older.user_id
 AND newer.position_id = older.position_id
 AND newer.id > older.id
 AND newer.deleted = 0
 AND newer.archived = 0
 AND newer.status NOT IN ('ACCEPTED', 'REJECTED', 'WITHDRAWN', 'CLOSED')
SET older.archived = 1,
    older.updated_at = CURRENT_TIMESTAMP
WHERE older.deleted = 0
  AND older.archived = 0
  AND older.status NOT IN ('ACCEPTED', 'REJECTED', 'WITHDRAWN', 'CLOSED');

ALTER TABLE jt_job_application
    ADD COLUMN active_position_key VARCHAR(100)
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted = 0
                     AND archived = 0
                     AND status NOT IN ('ACCEPTED', 'REJECTED', 'WITHDRAWN', 'CLOSED')
                THEN CONCAT(user_id, ':', position_id)
                ELSE NULL
            END
        ) STORED,
    ADD UNIQUE KEY uk_application_active_position (active_position_key),
    ADD KEY idx_app_user_archived_updated (user_id, archived, updated_at);

CREATE TABLE IF NOT EXISTS jt_application_idempotency (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    target_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    result_status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    result_version INT NULL,
    result_to_status VARCHAR(30) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_application_idempotency (user_id, application_id, idempotency_key),
    KEY idx_idempotency_application (user_id, application_id, created_at),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES jt_user(id),
    CONSTRAINT fk_idempotency_application FOREIGN KEY (application_id) REFERENCES jt_job_application(id)
) ENGINE=InnoDB COMMENT='投递状态流转数据库幂等记录';
