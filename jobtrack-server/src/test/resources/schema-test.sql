-- H2 测试用建表脚本 (MySQL 兼容模式)
CREATE TABLE IF NOT EXISTS jt_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(60) NOT NULL,
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_user_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    default_reminder_minutes INT NOT NULL DEFAULT 60,
    week_start TINYINT NOT NULL DEFAULT 1,
    email_notification TINYINT NOT NULL DEFAULT 0,
    theme VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    short_name VARCHAR(80),
    industry VARCHAR(80),
    scale VARCHAR(30),
    city VARCHAR(80),
    website VARCHAR(500),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    city VARCHAR(80),
    work_type VARCHAR(30) NOT NULL DEFAULT 'INTERNSHIP',
    workplace_type VARCHAR(30) NOT NULL DEFAULT 'ONSITE',
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    salary_unit VARCHAR(20),
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    source VARCHAR(50),
    source_url VARCHAR(1000),
    description TEXT,
    requirements TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    published_at TIMESTAMP,
    deadline_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    note VARCHAR(500),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_job_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    resume_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'SAVED',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(50),
    applied_at TIMESTAMP,
    referral_name VARCHAR(100),
    expected_salary_min DECIMAL(12,2),
    expected_salary_max DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
    next_action VARCHAR(255),
    next_action_at TIMESTAMP,
    note TEXT,
    archived TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_application_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    change_note VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_interview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    round_no INT NOT NULL DEFAULT 1,
    title VARCHAR(150) NOT NULL,
    interview_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    location VARCHAR(500),
    meeting_url VARCHAR(1000),
    interviewer VARCHAR(200),
    result VARCHAR(30),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    biz_type VARCHAR(30) NOT NULL,
    biz_id BIGINT,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(1000),
    remind_at TIMESTAMP NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    fail_reason VARCHAR(500),
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jt_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    module VARCHAR(50) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    request_id VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    duration_ms BIGINT,
    success TINYINT NOT NULL DEFAULT 1,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
