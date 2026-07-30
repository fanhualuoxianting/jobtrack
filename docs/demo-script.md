# 5—8 分钟演示脚本

演示账号仅限开发环境：`demo@jobtrack.local / JobTrack@123456`。先按 README 启动开发环境，默认 Dashboard 日期可看到分散在 2026-06 至 2026-08 的演示数据。

1. 登录：展示 Axios 会话恢复、内存 Access Token 和 HttpOnly Refresh Cookie。
2. 查看 Dashboard：切换来源、公司、粒度和归档筛选；说明统计来自状态历史，Redis 只缓存结果且可回源。
3. 公司/岗位：新建一家公司和 Java 实习岗位；说明所有查询带 `user_id`，重复名称/岗位组合由服务和数据库共同保护。
4. 简历：上传 PDF/DOCX 版本并设为默认；说明真实结构校验、SHA-256 去重、临时文件和事务补偿。
5. 投递：创建投递后用状态机流转 `SAVED -> APPLIED -> INTERVIEWING`；说明 expectedVersion 和幂等键解决并发与重试。
6. 时间线：打开投递时间线，展示历史记录而不是只看当前 status。
7. 面试：安排多轮面试，展示自动生成的 24H/1H 提醒。
8. 通知：打开通知中心，展示 SSE 实时连接和未读数量；最后回到 Dashboard 查看统计更新。

演示回答重点：为什么单体、为什么 JWT + Rotation、为什么 SSE、提醒如何避免多实例重复发送、Redis 故障时为何仍可用。
