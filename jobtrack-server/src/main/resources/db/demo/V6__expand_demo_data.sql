-- V6: 扩充开发演示数据（仅 dev profile 加载；不进入生产迁移）
-- 只使用固定主键和存在性判断，重复启动不会产生重复演示记录。

-- 10 家公司（已有 1-3）
INSERT INTO jt_company (id, user_id, name, short_name, industry, scale, city, website, description)
SELECT id, 1, name, short_name, industry, scale, city, website, description
FROM (
    SELECT 4 id, '星河软件有限公司' name, '星河软件' short_name, '软件服务' industry, '100-499人' scale, '杭州' city, 'https://star.example.com' website, '企业软件与研发效能' description
    UNION ALL SELECT 5, '远景云计算有限公司', '远景云', '云计算', '500-999人', '深圳', 'https://cloud.example.com', '云原生平台方向'
    UNION ALL SELECT 6, '微光教育科技有限公司', '微光教育', '教育科技', '50-99人', '成都', 'https://edu.example.com', '在线教育产品研发'
    UNION ALL SELECT 7, '北辰智能制造有限公司', '北辰智能', '智能制造', '1000-9999人', '苏州', 'https://smart.example.com', '工业互联网平台'
    UNION ALL SELECT 8, '青禾金融科技有限公司', '青禾金融', '金融科技', '500-999人', '广州', 'https://fin.example.com', '风控与数据工程'
    UNION ALL SELECT 9, '海岳物流科技有限公司', '海岳物流', '物流科技', '100-499人', '武汉', 'https://logistics.example.com', '供应链数字化'
    UNION ALL SELECT 10, '澄明医疗科技有限公司', '澄明医疗', '医疗科技', '100-499人', '重庆', 'https://health.example.com', '医疗信息化产品'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_company c WHERE c.id = seed.id);

-- 20 个岗位（已有 1-5）
INSERT INTO jt_position (id, user_id, company_id, title, department, city, work_type, workplace_type, salary_min, salary_max, salary_unit, currency, source, status)
SELECT id, 1, company_id, title, department, city, 'INTERNSHIP', workplace_type, salary_min, salary_max, 'DAY', 'CNY', source, status
FROM (
    SELECT 6 id, 4 company_id, 'Java 服务端实习生' title, '研发中心' department, '杭州' city, 'HYBRID' workplace_type, 180.00 salary_min, 280.00 salary_max, 'BOSS' source, 'OPEN' status
    UNION ALL SELECT 7, 5, '云原生开发实习生', '云平台部', '深圳', 'REMOTE', 220.00, 320.00, '内推', 'OPEN'
    UNION ALL SELECT 8, 6, '后端工程实习生', '技术部', '成都', 'ONSITE', 120.00, 220.00, '官网', 'OPEN'
    UNION ALL SELECT 9, 7, '工业互联网开发实习生', '平台部', '苏州', 'HYBRID', 200.00, 300.00, '招聘会', 'OPEN'
    UNION ALL SELECT 10, 8, '风控开发实习生', '风控技术部', '广州', 'ONSITE', 220.00, 350.00, '官网', 'OPEN'
    UNION ALL SELECT 11, 9, '数据工程实习生', '数据平台部', '武汉', 'HYBRID', 160.00, 260.00, 'BOSS', 'OPEN'
    UNION ALL SELECT 12, 10, '医疗软件开发实习生', '产品研发部', '重庆', 'ONSITE', 150.00, 240.00, '官网', 'OPEN'
    UNION ALL SELECT 13, 4, '测试开发实习生', '质量部', '杭州', 'ONSITE', 140.00, 230.00, '校园招聘', 'OPEN'
    UNION ALL SELECT 14, 5, 'Java 中间件实习生', '基础架构部', '深圳', 'HYBRID', 210.00, 330.00, '内推', 'OPEN'
    UNION ALL SELECT 15, 6, '数据分析实习生', '数据部', '成都', 'REMOTE', 130.00, 210.00, 'BOSS', 'CLOSED'
    UNION ALL SELECT 16, 7, '平台工程实习生', '平台部', '苏州', 'HYBRID', 180.00, 280.00, '官网', 'OPEN'
    UNION ALL SELECT 17, 8, '数据风控实习生', '风控技术部', '广州', 'ONSITE', 200.00, 320.00, '内推', 'OPEN'
    UNION ALL SELECT 18, 9, '物流算法实习生', '算法部', '武汉', 'REMOTE', 180.00, 300.00, '官网', 'OPEN'
    UNION ALL SELECT 19, 10, '医疗数据实习生', '数据产品部', '重庆', 'HYBRID', 160.00, 260.00, '校园招聘', 'OPEN'
    UNION ALL SELECT 20, 7, '制造数据实习生', '数据平台部', '苏州', 'ONSITE', 170.00, 270.00, 'BOSS', 'OPEN'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_position p WHERE p.id = seed.id);

-- 3 个演示简历版本。文件内容由开发者通过页面上传或 scripts/smoke-test.ps1 生成；数据库记录用于展示版本关联。
INSERT INTO jt_resume (id, user_id, version_name, original_file_name, storage_key, mime_type, file_size, sha256, is_default, note)
SELECT id, 1, version_name, original_file_name, storage_key, 'application/pdf', file_size, sha256, is_default, note
FROM (
    SELECT 1 id, 'Java 后端版' version_name, 'demo-backend.pdf' original_file_name, 'demo/1/backend.pdf' storage_key, 633 file_size, REPEAT('1', 64) sha256, 1 is_default, '突出 Spring Boot、MySQL、Redis 项目经验' note
    UNION ALL SELECT 2, '数据平台版', 'demo-data.pdf', 'demo/1/data.pdf', 633, REPEAT('2', 64), 0, '突出 SQL 聚合和数据处理能力'
    UNION ALL SELECT 3, '通用实习版', 'demo-general.pdf', 'demo/1/general.pdf', 633, REPEAT('3', 64), 0, '通用投递版本'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_resume r WHERE r.id = seed.id);

-- 26 条投递：覆盖草稿、已投递、测评、面试、Offer、接受 Offer、拒绝、主动撤回和归档。
INSERT INTO jt_job_application (id, user_id, company_id, position_id, resume_id, status, priority, source, applied_at, note, archived)
SELECT id, 1, company_id, position_id,
       CASE WHEN EXISTS (SELECT 1 FROM jt_resume r WHERE r.id = seed.resume_id AND r.user_id = 1 AND r.deleted = 0)
            THEN resume_id ELSE NULL END,
       status, priority, source, applied_at, note, archived
FROM (
    SELECT 7 id, 4 company_id, 6 position_id, 1 resume_id, 'SAVED' status, 'LOW' priority, 'BOSS' source, NULL applied_at, '准备完善项目描述' note, 0 archived
    UNION ALL SELECT 8, 5, 7, 1, 'APPLIED', 'HIGH', '内推', '2026-06-03 10:00:00', '等待技术笔试', 0
    UNION ALL SELECT 9, 6, 8, 2, 'ASSESSMENT', 'MEDIUM', '官网', '2026-06-12 14:00:00', '测评已提交', 0
    UNION ALL SELECT 10, 7, 9, 1, 'INTERVIEWING', 'HIGH', '招聘会', '2026-06-18 09:30:00', '准备业务面', 0
    UNION ALL SELECT 11, 8, 10, 2, 'OFFERED', 'HIGH', '官网', '2026-06-25 11:00:00', '等待 Offer 细节', 0
    UNION ALL SELECT 12, 9, 11, 3, 'ACCEPTED', 'HIGH', '内推', '2026-06-28 15:00:00', '已接受实习 Offer', 0
    UNION ALL SELECT 13, 10, 12, 1, 'REJECTED', 'MEDIUM', '官网', '2026-07-02 09:00:00', '岗位方向不匹配', 0
    UNION ALL SELECT 14, 4, 13, 2, 'WITHDRAWN', 'LOW', '校园招聘', '2026-07-05 13:00:00', '主动撤回', 0
    UNION ALL SELECT 15, 5, 14, 1, 'APPLIED', 'MEDIUM', 'BOSS', '2026-07-08 10:00:00', '等待简历筛选', 0
    UNION ALL SELECT 16, 6, 15, 2, 'ASSESSMENT', 'HIGH', 'BOSS', '2026-07-12 16:00:00', '准备在线测评', 0
    UNION ALL SELECT 17, 4, 16, 3, 'INTERVIEWING', 'HIGH', '内推', '2026-07-15 10:00:00', '已进入技术面', 0
    UNION ALL SELECT 18, 5, 17, 1, 'OFFERED', 'HIGH', '官网', '2026-07-18 14:00:00', '比较实习地点', 0
    UNION ALL SELECT 19, 6, 18, 2, 'ACCEPTED', 'HIGH', '校园招聘', '2026-07-20 09:00:00', '已确认入职', 0
    UNION ALL SELECT 20, 7, 19, 3, 'REJECTED', 'MEDIUM', 'BOSS', '2026-07-22 11:00:00', '流程结束', 0
    UNION ALL SELECT 21, 8, 20, 1, 'WITHDRAWN', 'LOW', '官网', '2026-07-24 15:00:00', '时间安排冲突', 0
    UNION ALL SELECT 22, 9, 11, 2, 'APPLIED', 'MEDIUM', '内推', '2026-07-26 10:00:00', '等待反馈', 0
    UNION ALL SELECT 23, 10, 12, 3, 'INTERVIEWING', 'HIGH', '官网', '2026-07-28 13:00:00', '即将进行二面', 0
    UNION ALL SELECT 24, 4, 13, 1, 'OFFERED', 'HIGH', '招聘会', '2026-07-30 09:00:00', '准备薪资沟通', 0
    UNION ALL SELECT 25, 5, 20, 2, 'SAVED', 'LOW', 'BOSS', NULL, '收藏待补充信息', 0
    UNION ALL SELECT 26, 6, 15, 3, 'APPLIED', 'MEDIUM', '官网', '2026-06-20 12:00:00', '已归档的历史投递', 1
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_job_application a WHERE a.id = seed.id);

-- 新增投递的最小历史链，保证 Dashboard 漏斗/周期统计可展示。
INSERT INTO jt_application_status_log (user_id, application_id, from_status, to_status, change_note, changed_at)
SELECT 1, application_id, NULL, 'APPLIED', '演示历史投递', changed_at
FROM (
    SELECT 8 application_id, '2026-06-03 10:00:00' changed_at UNION ALL SELECT 9, '2026-06-12 14:00:00' UNION ALL SELECT 10, '2026-06-18 09:30:00'
    UNION ALL SELECT 11, '2026-06-25 11:00:00' UNION ALL SELECT 12, '2026-06-28 15:00:00' UNION ALL SELECT 13, '2026-07-02 09:00:00'
    UNION ALL SELECT 14, '2026-07-05 13:00:00' UNION ALL SELECT 15, '2026-07-08 10:00:00' UNION ALL SELECT 16, '2026-07-12 16:00:00'
    UNION ALL SELECT 17, '2026-07-15 10:00:00' UNION ALL SELECT 18, '2026-07-18 14:00:00' UNION ALL SELECT 19, '2026-07-20 09:00:00'
    UNION ALL SELECT 20, '2026-07-22 11:00:00' UNION ALL SELECT 21, '2026-07-24 15:00:00' UNION ALL SELECT 22, '2026-07-26 10:00:00'
    UNION ALL SELECT 23, '2026-07-28 13:00:00' UNION ALL SELECT 24, '2026-07-30 09:00:00' UNION ALL SELECT 26, '2026-06-20 12:00:00'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id = seed.application_id AND l.to_status = 'APPLIED');

INSERT INTO jt_application_status_log (user_id, application_id, from_status, to_status, change_note, changed_at)
SELECT 1, application_id, 'APPLIED', to_status, '演示状态历史', changed_at
FROM (
    SELECT 9 application_id, 'ASSESSMENT' to_status, '2026-06-14 10:00:00' changed_at UNION ALL SELECT 10, 'INTERVIEWING', '2026-06-25 16:00:00'
    UNION ALL SELECT 11, 'INTERVIEWING', '2026-07-02 16:00:00' UNION ALL SELECT 11, 'OFFERED', '2026-07-15 11:00:00'
    UNION ALL SELECT 12, 'INTERVIEWING', '2026-07-05 14:00:00' UNION ALL SELECT 12, 'OFFERED', '2026-07-18 11:00:00' UNION ALL SELECT 12, 'ACCEPTED', '2026-07-22 11:00:00'
    UNION ALL SELECT 13, 'REJECTED', '2026-07-10 09:00:00' UNION ALL SELECT 14, 'WITHDRAWN', '2026-07-09 09:00:00'
    UNION ALL SELECT 16, 'ASSESSMENT', '2026-07-14 10:00:00' UNION ALL SELECT 17, 'INTERVIEWING', '2026-07-23 15:00:00'
    UNION ALL SELECT 18, 'INTERVIEWING', '2026-07-24 15:00:00' UNION ALL SELECT 18, 'OFFERED', '2026-07-29 15:00:00'
    UNION ALL SELECT 19, 'INTERVIEWING', '2026-07-23 10:00:00' UNION ALL SELECT 19, 'OFFERED', '2026-07-28 10:00:00' UNION ALL SELECT 19, 'ACCEPTED', '2026-07-30 10:00:00'
    UNION ALL SELECT 20, 'REJECTED', '2026-07-29 11:00:00' UNION ALL SELECT 21, 'WITHDRAWN', '2026-07-27 09:00:00'
    UNION ALL SELECT 23, 'INTERVIEWING', '2026-07-29 14:00:00' UNION ALL SELECT 24, 'INTERVIEWING', '2026-07-30 14:00:00' UNION ALL SELECT 24, 'OFFERED', '2026-07-30 18:00:00'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_application_status_log l WHERE l.application_id = seed.application_id AND l.to_status = seed.to_status);

-- 多轮面试：已有 3 场，新增 4 场用于演示列表和提醒。
INSERT INTO jt_interview (id, user_id, application_id, round_no, round_name, title, interview_type, status, scheduled_start, scheduled_end, timezone, meeting_url, interviewer, note)
SELECT id, 1, application_id, round_no, round_name, title, interview_type, 'SCHEDULED', scheduled_start, scheduled_end, 'Asia/Shanghai', meeting_url, interviewer, note
FROM (
    SELECT 4 id, 10 application_id, 1 round_no, '业务一面' round_name, '工业互联网业务面' title, 'ONLINE' interview_type, '2026-08-06 14:00:00' scheduled_start, '2026-08-06 15:00:00' scheduled_end, 'https://meeting.example.com/demo4' meeting_url, '研发经理' interviewer, '准备项目复盘' note
    UNION ALL SELECT 5, 17, 1, '技术一面', 'Java 服务端技术面', 'ONLINE', '2026-08-08 10:00:00', '2026-08-08 11:00:00', 'https://meeting.example.com/demo5', '技术负责人', '准备并发和数据库'
    UNION ALL SELECT 6, 23, 1, 'HR 面', '医疗软件 HR 面', 'PHONE', '2026-08-10 16:00:00', '2026-08-10 16:30:00', NULL, 'HR', '确认实习时间'
    UNION ALL SELECT 7, 24, 2, '终面', '招聘会岗位终面', 'ONLINE', '2026-08-12 09:30:00', '2026-08-12 10:30:00', 'https://meeting.example.com/demo7', '部门主管', '准备薪资沟通'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_interview i WHERE i.id = seed.id);

-- 待发送、已发送未读、已读提醒各至少一条。
INSERT INTO jt_reminder (user_id, interview_id, application_id, reminder_type, scheduled_at, biz_type, biz_id, title, content, remind_at, channel, status, sent_at, read_at, idempotency_key)
SELECT 1, interview_id, application_id, reminder_type, remind_at, 'INTERVIEW', interview_id, title, content, remind_at, 'IN_APP', status, sent_at, read_at, idempotency_key
FROM (
    SELECT 4 interview_id, 10 application_id, '24H' reminder_type, '面试提醒：工业互联网业务面' title, '后天 14:00 有业务一面，请提前准备' content, '2026-08-05 14:00:00' remind_at, 'PENDING' status, NULL sent_at, NULL read_at, 'DEMO:INTERVIEW:4:24H' idempotency_key
    UNION ALL SELECT 5, 17, '24H', '面试提醒：Java 服务端技术面', '明天 10:00 有技术一面', '2026-08-07 10:00:00', 'SENT', '2026-08-07 09:00:00', NULL, 'DEMO:INTERVIEW:5:24H'
    UNION ALL SELECT 6, 23, '1H', '面试提醒：医疗软件 HR 面', '今天 16:00 有 HR 面', '2026-08-10 15:00:00', 'SENT', '2026-08-10 15:00:00', '2026-08-10 17:00:00', 'DEMO:INTERVIEW:6:1H'
    UNION ALL SELECT 7, 24, '24H', '面试提醒：招聘会岗位终面', '明天 09:30 有终面', '2026-08-11 09:30:00', 'PENDING', NULL, NULL, 'DEMO:INTERVIEW:7:24H'
) seed
WHERE NOT EXISTS (SELECT 1 FROM jt_reminder r WHERE r.idempotency_key = seed.idempotency_key);
