# 简历项目描述

## 项目简介

JobTrack 是面向高校学生求职场景的前后端分离投递管理平台，覆盖公司/岗位、简历版本、投递状态机、多轮面试、提醒通知和统计 Dashboard。项目采用模块化单体，支持本地开发和 Docker 完整部署。

## 技术栈

Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、Flyway、MySQL 8.4、Redis 8、Vue 3、TypeScript、Vite、Element Plus、ECharts、Docker、Nginx。

## 项目亮点

- 设计投递领域状态机，将状态校验、历史记录和事务后事件集中管理，并通过乐观锁与数据库幂等键解决并发流转和网络重试导致的重复更新。
- 使用 Refresh Token Rotation + HttpOnly Cookie，旧令牌重放会撤销会话并写安全审计；登录连续失败触发窗口限流。
- 对 PDF/DOCX 做扩展名、MIME、真实结构和 SHA-256 联合校验，结合事务同步与补偿队列处理数据库/文件系统不一致。
- 使用带 due 条件的数据库原子 UPDATE 抢占提醒，配合 SSE 实时通知和连接上限，保证多实例扫描不会重复发送通知事实。
- 基于状态历史实现漏斗、转化率、趋势和周期统计，Redis 使用用户版本键在写事务提交后失效，Redis 故障时回源数据库。
- 采用 Testcontainers、前端行为测试、Docker Compose、Nginx SSE 配置和 GitHub Actions 建立可重复交付链。
