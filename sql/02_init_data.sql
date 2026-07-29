-- JobTrack 初始化演示数据
-- 演示账号：demo@jobtrack.local / JobTrack@123456
-- 密码使用 BCrypt 哈希存储

USE jobtrack;

-- 演示用户 (密码: JobTrack@123456)
INSERT INTO jt_user (id, username, email, nickname, password_hash, role, status)
VALUES (1, 'demo', 'demo@jobtrack.local', '演示用户',
        '$2a$10$DWSJgvVkvU1oCOAbrLuJ6.CfA2oFDgyvSLW1T.15vYehVLPbAzBu6',
        'USER', 'ACTIVE');

-- 用户设置
INSERT INTO jt_user_setting (user_id, timezone, default_reminder_minutes, week_start, email_notification, theme)
VALUES (1, 'Asia/Shanghai', 60, 1, 0, 'SYSTEM');

-- 三家公司
INSERT INTO jt_company (id, user_id, name, short_name, industry, scale, city, website, description) VALUES
(1, 1, '示例科技有限公司', '示例科技', '互联网', '500-999人', '南京', 'https://example.com', '重点关注 Java 后端实习岗位'),
(2, 1, '云端数据股份公司', '云端数据', '大数据', '1000-9999人', '上海', 'https://clouddata.example.com', '数据平台方向'),
(3, 1, '智联未来信息技术公司', '智联未来', '人工智能', '100-499人', '北京', 'https://ai-future.example.com', 'AI 应用开发');

-- 五个岗位
INSERT INTO jt_position (id, user_id, company_id, title, department, city, work_type, workplace_type, salary_min, salary_max, salary_unit, currency, source, status) VALUES
(1, 1, 1, 'Java 后端开发实习生', '平台研发部', '南京', 'INTERNSHIP', 'ONSITE', 150.00, 250.00, 'DAY', 'CNY', 'BOSS', 'OPEN'),
(2, 1, 1, '测试开发实习生', '质量保障部', '南京', 'INTERNSHIP', 'ONSITE', 120.00, 200.00, 'DAY', 'CNY', '官网', 'OPEN'),
(3, 1, 2, '数据平台开发实习生', '数据工程部', '上海', 'INTERNSHIP', 'HYBRID', 200.00, 300.00, 'DAY', 'CNY', '内推', 'OPEN'),
(4, 1, 3, 'AI 应用开发实习生', '算法工程部', '北京', 'INTERNSHIP', 'REMOTE', 250.00, 400.00, 'DAY', 'CNY', '官网', 'OPEN'),
(5, 1, 2, '后端开发实习生', '基础架构部', '上海', 'INTERNSHIP', 'ONSITE', 180.00, 280.00, 'DAY', 'CNY', '招聘会', 'CLOSED');

-- 六条不同状态的投递记录
INSERT INTO jt_job_application (id, user_id, company_id, position_id, resume_id, status, priority, source, applied_at, note, archived) VALUES
(1, 1, 1, 1, NULL, 'INTERVIEWING', 'HIGH', 'BOSS', '2026-07-10 10:00:00', '准备技术一面', 0),
(2, 1, 1, 2, NULL, 'APPLIED', 'MEDIUM', '官网', '2026-07-15 14:30:00', '等待笔试通知', 0),
(3, 1, 2, 3, NULL, 'ASSESSMENT', 'HIGH', '内推', '2026-07-18 09:00:00', '在线测评已完成', 0),
(4, 1, 3, 4, NULL, 'SAVED', 'LOW', '官网', NULL, '先收藏，后续再投', 0),
(5, 1, 2, 5, NULL, 'REJECTED', 'MEDIUM', '招聘会', '2026-07-05 16:00:00', '简历未通过筛选', 0),
(6, 1, 1, 1, NULL, 'OFFERED', 'HIGH', 'BOSS', '2026-07-01 10:00:00', '已获得 Offer，考虑中', 0);

-- 状态日志
INSERT INTO jt_application_status_log (user_id, application_id, from_status, to_status, change_note, changed_at) VALUES
(1, 1, NULL, 'SAVED', '收藏岗位', '2026-07-08 09:00:00'),
(1, 1, 'SAVED', 'APPLIED', '通过 BOSS 直聘投递', '2026-07-10 10:00:00'),
(1, 1, 'APPLIED', 'INTERVIEWING', '收到技术一面通知', '2026-07-20 17:00:00'),
(1, 2, NULL, 'APPLIED', '官网投递', '2026-07-15 14:30:00'),
(1, 3, NULL, 'APPLIED', '内推投递', '2026-07-18 09:00:00'),
(1, 3, 'APPLIED', 'ASSESSMENT', '收到在线测评链接', '2026-07-19 10:00:00'),
(1, 5, NULL, 'APPLIED', '招聘会现场投递', '2026-07-05 16:00:00'),
(1, 5, 'APPLIED', 'REJECTED', '简历未通过', '2026-07-12 11:00:00'),
(1, 6, NULL, 'APPLIED', 'BOSS 投递', '2026-07-01 10:00:00'),
(1, 6, 'APPLIED', 'INTERVIEWING', '电话面试', '2026-07-05 14:00:00'),
(1, 6, 'INTERVIEWING', 'OFFERED', '面试通过，发放 Offer', '2026-07-22 16:00:00');

-- 三场未来面试
INSERT INTO jt_interview (id, user_id, application_id, round_no, title, interview_type, status, scheduled_start, scheduled_end, timezone, meeting_url, interviewer, note) VALUES
(1, 1, 1, 1, 'Java 技术一面', 'ONLINE', 'SCHEDULED', '2026-08-02 14:00:00', '2026-08-02 15:00:00', 'Asia/Shanghai', 'https://meeting.example.com/abc123', '研发工程师', '重点准备集合、MySQL索引、Spring IOC'),
(2, 1, 1, 2, 'Java 技术二面', 'ONLINE', 'SCHEDULED', '2026-08-05 10:00:00', '2026-08-05 11:00:00', 'Asia/Shanghai', 'https://meeting.example.com/def456', '技术主管', '系统设计、项目经验'),
(3, 1, 3, 1, '数据平台技术面', 'PHONE', 'SCHEDULED', '2026-08-03 16:00:00', '2026-08-03 16:45:00', 'Asia/Shanghai', NULL, 'HR', '电话初筛');

-- 提醒
INSERT INTO jt_reminder (user_id, biz_type, biz_id, title, content, remind_at, channel, status, idempotency_key) VALUES
(1, 'INTERVIEW', 1, '面试提醒：Java 技术一面', '明天 14:00 有 Java 技术一面，请提前准备', '2026-08-02 13:00:00', 'IN_APP', 'PENDING', 'INTERVIEW:1:1785682800'),
(1, 'INTERVIEW', 2, '面试提醒：Java 技术二面', '8月5日 10:00 有技术二面', '2026-08-05 09:00:00', 'IN_APP', 'PENDING', 'INTERVIEW:2:1785934800'),
(1, 'INTERVIEW', 3, '面试提醒：数据平台技术面', '8月3日 16:00 电话面试', '2026-08-03 15:00:00', 'IN_APP', 'PENDING', 'INTERVIEW:3:1785769200');
