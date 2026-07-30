-- V1: JobTrack 基础表结构（对已有库幂等执行）
-- 字符集 utf8mb4；所有业务表带 created_at/updated_at/deleted，关键表带 version 乐观锁

-- 用户表
CREATE TABLE IF NOT EXISTS jt_user (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(120) NOT NULL COMMENT '邮箱',
    phone VARCHAR(30) NULL COMMENT '手机号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    nickname VARCHAR(60) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) NULL COMMENT '头像地址',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    KEY idx_user_status (status)
) ENGINE=InnoDB COMMENT='用户表';

-- 登录会话表（Refresh Token Rotation 的服务端状态）
CREATE TABLE IF NOT EXISTS jt_auth_session (
    id VARCHAR(36) PRIMARY KEY COMMENT '会话ID (UUID 去掉横线)',
    user_id BIGINT UNSIGNED NOT NULL,
    device_name VARCHAR(120) NULL COMMENT '设备名称',
    user_agent VARCHAR(500) NULL,
    ip_address VARCHAR(64) NULL,
    refresh_token_hash VARCHAR(64) NOT NULL COMMENT '当前有效 Refresh Token 的 SHA-256 哈希',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    expires_at DATETIME NOT NULL COMMENT '会话过期时间',
    revoked_at DATETIME NULL COMMENT '注销时间，NULL 表示会话有效',
    KEY idx_session_user (user_id, revoked_at),
    KEY idx_session_expires (expires_at),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES jt_user(id)
) ENGINE=InnoDB COMMENT='登录会话';

-- 用户设置
CREATE TABLE IF NOT EXISTS jt_user_setting (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '用户时区',
    default_reminder_minutes INT NOT NULL DEFAULT 60 COMMENT '默认提前提醒分钟数',
    week_start TINYINT NOT NULL DEFAULT 1 COMMENT '每周开始日，1表示周一',
    email_notification TINYINT NOT NULL DEFAULT 0,
    theme VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_setting_user (user_id),
    CONSTRAINT fk_setting_user FOREIGN KEY (user_id) REFERENCES jt_user(id)
) ENGINE=InnoDB COMMENT='用户设置';

-- 公司表（唯一性由 Service 层校验 deleted=0 下同名不可重复）
CREATE TABLE IF NOT EXISTS jt_company (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(150) NOT NULL COMMENT '公司名称',
    short_name VARCHAR(80) NULL COMMENT '简称',
    industry VARCHAR(80) NULL COMMENT '行业',
    scale VARCHAR(30) NULL COMMENT '公司规模',
    city VARCHAR(80) NULL COMMENT '所在城市',
    website VARCHAR(500) NULL COMMENT '官方网站',
    description TEXT NULL COMMENT '公司备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_company_user_name (user_id, name),
    KEY idx_company_user_city (user_id, city),
    KEY idx_company_user_industry (user_id, industry),
    CONSTRAINT fk_company_user FOREIGN KEY (user_id) REFERENCES jt_user(id)
) ENGINE=InnoDB COMMENT='用户维护的公司';

-- 岗位表
CREATE TABLE IF NOT EXISTS jt_position (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    company_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(150) NOT NULL COMMENT '岗位名称',
    department VARCHAR(100) NULL COMMENT '部门',
    city VARCHAR(80) NULL COMMENT '工作城市',
    work_type VARCHAR(30) NOT NULL DEFAULT 'INTERNSHIP' COMMENT 'INTERNSHIP/FULL_TIME/PART_TIME',
    workplace_type VARCHAR(30) NOT NULL DEFAULT 'ONSITE' COMMENT 'ONSITE/REMOTE/HYBRID',
    salary_min DECIMAL(12,2) NULL,
    salary_max DECIMAL(12,2) NULL,
    salary_unit VARCHAR(20) NULL COMMENT 'DAY/MONTH/YEAR',
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    source VARCHAR(50) NULL COMMENT 'BOSS/官网/内推等',
    source_url VARCHAR(1000) NULL COMMENT '岗位链接',
    description MEDIUMTEXT NULL COMMENT '岗位描述',
    requirements MEDIUMTEXT NULL COMMENT '岗位要求',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    published_at DATETIME NULL,
    deadline_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_position_user_company (user_id, company_id),
    KEY idx_position_user_status (user_id, status),
    KEY idx_position_user_city (user_id, city),
    KEY idx_position_deadline (deadline_at),
    CONSTRAINT fk_position_user FOREIGN KEY (user_id) REFERENCES jt_user(id),
    CONSTRAINT fk_position_company FOREIGN KEY (company_id) REFERENCES jt_company(id)
) ENGINE=InnoDB COMMENT='岗位信息';

-- 简历版本
CREATE TABLE IF NOT EXISTS jt_resume (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    version_name VARCHAR(100) NOT NULL COMMENT '简历版本名称',
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL COMMENT '存储路径或对象存储Key',
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL COMMENT '字节数',
    sha256 VARCHAR(64) NOT NULL COMMENT '文件摘要',
    is_default TINYINT NOT NULL DEFAULT 0,
    note VARCHAR(500) NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_resume_user_default (user_id, is_default),
    KEY idx_resume_user_uploaded (user_id, uploaded_at),
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES jt_user(id)
) ENGINE=InnoDB COMMENT='简历版本';

-- 投递记录
CREATE TABLE IF NOT EXISTS jt_job_application (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    company_id BIGINT UNSIGNED NOT NULL,
    position_id BIGINT UNSIGNED NOT NULL,
    resume_id BIGINT UNSIGNED NULL COMMENT '投递所用简历',
    status VARCHAR(30) NOT NULL DEFAULT 'SAVED',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW/MEDIUM/HIGH',
    source VARCHAR(50) NULL COMMENT '投递渠道',
    applied_at DATETIME NULL,
    referral_name VARCHAR(100) NULL COMMENT '内推人',
    expected_salary_min DECIMAL(12,2) NULL,
    expected_salary_max DECIMAL(12,2) NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    next_action VARCHAR(255) NULL COMMENT '下一步行动',
    next_action_at DATETIME NULL,
    note TEXT NULL,
    archived TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_app_user_status (user_id, status),
    KEY idx_app_user_company (user_id, company_id),
    KEY idx_app_user_position (user_id, position_id),
    KEY idx_app_user_applied_at (user_id, applied_at),
    KEY idx_app_user_next_action (user_id, next_action_at),
    KEY idx_app_dashboard (user_id, archived, status, created_at),
    CONSTRAINT fk_app_user FOREIGN KEY (user_id) REFERENCES jt_user(id),
    CONSTRAINT fk_app_company FOREIGN KEY (company_id) REFERENCES jt_company(id),
    CONSTRAINT fk_app_position FOREIGN KEY (position_id) REFERENCES jt_position(id),
    CONSTRAINT fk_app_resume FOREIGN KEY (resume_id) REFERENCES jt_resume(id)
) ENGINE=InnoDB COMMENT='投递记录';

-- 投递状态变化日志
CREATE TABLE IF NOT EXISTS jt_application_status_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    change_note VARCHAR(500) NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_status_log_app_time (application_id, changed_at),
    KEY idx_status_log_user_time (user_id, changed_at),
    CONSTRAINT fk_status_log_user FOREIGN KEY (user_id) REFERENCES jt_user(id),
    CONSTRAINT fk_status_log_app FOREIGN KEY (application_id) REFERENCES jt_job_application(id)
) ENGINE=InnoDB COMMENT='投递状态变化日志';

-- 面试日程
CREATE TABLE IF NOT EXISTS jt_interview (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    round_no INT NOT NULL DEFAULT 1 COMMENT '面试轮次',
    title VARCHAR(150) NOT NULL COMMENT '例如 Java技术一面',
    interview_type VARCHAR(30) NOT NULL COMMENT 'PHONE/ONLINE/ONSITE/WRITTEN/OTHER',
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_start DATETIME NOT NULL,
    scheduled_end DATETIME NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    location VARCHAR(500) NULL,
    meeting_url VARCHAR(1000) NULL,
    interviewer VARCHAR(200) NULL,
    result VARCHAR(30) NULL COMMENT 'PASS/FAIL/PENDING/UNKNOWN',
    note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    KEY idx_interview_user_start (user_id, scheduled_start),
    KEY idx_interview_app_round (application_id, round_no),
    KEY idx_interview_user_status (user_id, status),
    CONSTRAINT fk_interview_user FOREIGN KEY (user_id) REFERENCES jt_user(id),
    CONSTRAINT fk_interview_app FOREIGN KEY (application_id) REFERENCES jt_job_application(id)
) ENGINE=InnoDB COMMENT='面试日程';

-- 提醒与站内通知
CREATE TABLE IF NOT EXISTS jt_reminder (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    biz_type VARCHAR(30) NOT NULL COMMENT 'APPLICATION/INTERVIEW/CUSTOM',
    biz_id BIGINT UNSIGNED NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(1000) NULL,
    remind_at DATETIME NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at DATETIME NULL,
    read_at DATETIME NULL,
    fail_reason VARCHAR(500) NULL,
    idempotency_key VARCHAR(100) NULL COMMENT '避免重复发送',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_reminder_idempotency (idempotency_key),
    KEY idx_reminder_scan (status, remind_at),
    KEY idx_reminder_user_read (user_id, read_at, created_at),
    CONSTRAINT fk_reminder_user FOREIGN KEY (user_id) REFERENCES jt_user(id)
) ENGINE=InnoDB COMMENT='提醒与站内通知';

-- 操作日志（HTTP 级别留痕）
CREATE TABLE IF NOT EXISTS jt_operation_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    http_method VARCHAR(10) NULL,
    request_path VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    duration_ms BIGINT NULL,
    success TINYINT NOT NULL DEFAULT 1,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_user_time (user_id, created_at),
    KEY idx_operation_request_id (request_id)
) ENGINE=InnoDB COMMENT='操作日志';

-- 业务审计日志（登录、状态变更、文件操作等重要动作）
CREATE TABLE IF NOT EXISTS jt_audit_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL COMMENT '操作人ID，匿名操作为NULL',
    action VARCHAR(50) NOT NULL COMMENT '动作标识，如 LOGIN_SUCCESS',
    resource_type VARCHAR(50) NULL COMMENT '资源类型 COMPANY/APPLICATION/...',
    resource_id VARCHAR(64) NULL COMMENT '资源ID',
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAIL',
    detail VARCHAR(500) NULL COMMENT '附加说明（已脱敏）',
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    trace_id VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_user_time (user_id, created_at),
    KEY idx_audit_action_time (action, created_at),
    KEY idx_audit_trace (trace_id)
) ENGINE=InnoDB COMMENT='业务操作审计日志';
