# 阶段 6 收尾核查

## 测试环境

`InterviewIntegrationTest` 继承 `AbstractIntegrationTest`，每个测试类使用 Testcontainers 启动真实 MySQL 8.4 和 Redis 8，Flyway 执行与开发库相同的 V1、V3、V4 迁移。Redis 在测试基础设施中保持可用，但提醒扫描的去重事实只依赖 MySQL，不依赖 Redis。

路径：

- `jobtrack-server/src/test/java/com/fanhua/jobtrack/module/interview/InterviewIntegrationTest.java`
- `jobtrack-server/src/test/java/com/fanhua/jobtrack/module/reminder/ReminderDeliveryServiceTest.java`

## 完整测试清单

| 测试方法 | 覆盖风险 | 方式 |
| --- | --- | --- |
| `createInterviewAutoTransitionsAndBuildsReminders` | 创建面试、Asia/Tokyo 展示、首场面试自动进入 `INTERVIEWING`、状态历史、提醒生成 | Testcontainers MySQL/Redis + MockMvc |
| `invalidTimeZoneTimeAndDuplicateRound` | 非法时区、开始晚于结束、轮次大于 0、同一投递轮次唯一约束 | Testcontainers MySQL/Redis + MockMvc |
| `archivedAndExpiredReminderRules` | 归档投递拒绝安排面试；距离面试不足 24 小时只保留有效 1 小时提醒；过期提醒不生成 | Testcontainers MySQL/Redis + MockMvc + 原始 SQL 对照 |
| `newYorkDstKeepsInstantAndDisplayOffset` | `America/New_York` 夏令时切换时，UTC 存储、跨偏移展示和瞬时顺序 | Testcontainers MySQL/Redis + MockMvc |
| `updateInterviewOptimisticLockAndRebuildsReminders` | 乐观锁、旧待发送提醒取消、新提醒生成、旧版本修改拒绝 | Testcontainers MySQL/Redis + MockMvc + 原始 SQL |
| `cancelAndCompleteActions` | 取消提醒、完成结果/反馈、完成不自动进入 `OFFERED`/`REJECTED` | Testcontainers MySQL/Redis + MockMvc + 原始 SQL |
| `terminalAndOwnershipIsolation` | 终态投递拒绝创建；其他用户查询、修改、取消、完成均为 404 | Testcontainers MySQL/Redis + MockMvc |
| `failedAutoTransitionRollsBackInterviewAndReminders` | 自动流转数据库失败时，面试、提醒和投递状态一起回滚 | Testcontainers MySQL + 临时数据库触发器 + MockMvc |
| `concurrentScannerSendsOnce` | 两个扫描线程并发到期；只有一个数据库抢占成功；最终只有一个 `SENT`、一个未处理提醒 | Testcontainers MySQL/Redis + 并发线程 + 原始 SQL |
| `notificationCenterIsolationAndIdempotentRead` | 通知用户隔离、越权已读 404、重复已读幂等、未读数和列表 | Testcontainers MySQL/Redis + MockMvc |
| `sseConnectionLimitAndCleanup` | 单用户最多 3 条连接、超限行为、主动断开释放连接 | Testcontainers MySQL/Redis + `SseEmitter` |
| `failedNotificationDeliveryRecordsRetryCauseWithoutEscapingBatch` | 投递事件异常不向扫描批次逃逸，并记录重试原因 | Mockito 单元测试 |

阶段 6 主体此前的 8 项集成测试已经通过；本次补充 3 项集成测试和 1 项投递失败单元测试。真实脚本还验证过：创建面试 HTTP 201、投递自动变为 `INTERVIEWING`、通知 SSE 200、收到 `event:notification`，数据库同一面试的 1 小时提醒变为 `SENT`，通知未读数为 1。

## 状态语义

| 状态 | 语义 |
| --- | --- |
| `PENDING` | 已生成但尚未被扫描器处理的数据库提醒。 |
| `READY` | 扫描器通过原子 UPDATE 成功抢占，正在处理；不代表用户已在线收到 SSE。 |
| `SENT` | 通知事实已持久化到 `jt_reminder`，用户在线与否都算成功。 |
| `FAILED` | 本次处理失败且达到最大重试次数；当前最大次数为 3。 |
| `CANCELLED` | 面试取消/完成/删除后，未发送提醒不再处理。 |

`SENT` 的事实来源是数据库通知记录，不是 SSE。SSE 只是实时投递优化；用户离线、连接断开或 SSE 发送失败都不会把 `SENT` 改回 `FAILED`，客户端重连后通过通知列表补拉。当前重试针对数据库通知处理/事件发布异常，不针对 SSE 本身。

## 抢占 SQL

当前实现先查询候选 ID，再用数据库条件 UPDATE 抢占；关键 UPDATE 为：

```sql
UPDATE jt_reminder
SET status = 'READY', updated_at = UTC_TIMESTAMP()
WHERE id = ?
  AND status = 'PENDING'
  AND scheduled_at <= UTC_TIMESTAMP()
  AND deleted = 0;
```

只有返回影响行数为 1 的线程继续处理。并发测试证明同一到期提醒最终只有一条 `SENT`；Redis 不参与抢占，因此 Redis 不可用时不会改变数据库去重保证。失败分支依据 `retry_count + 1 >= max_retries` 进入 `FAILED`，否则回到 `PENDING` 并延迟下一次扫描。

## 开发 smoke 数据

查看当前开发库中的面试和通知：

```sql
SELECT id, user_id, application_id, round_name, status, scheduled_start
FROM jt_interview
ORDER BY id DESC
LIMIT 10;

SELECT id, user_id, interview_id, reminder_type, status, scheduled_at, read_at
FROM jt_reminder
WHERE biz_type = 'INTERVIEW'
ORDER BY id DESC
LIMIT 20;
```

当前真实联调新增的 1 条面试和 1 条已发送通知可以保留用于复核。清空并重新初始化开发库时运行：

```powershell
./scripts/reset-dev-data.ps1
```

脚本要求输入 `RESET-DEV-DATA`，或显式传 `-Force`。它只执行当前 Compose 项目的 `docker compose down -v` 和 `docker compose up -d mysql redis`，会删除 MySQL 数据卷和 Redis 数据卷，重新执行 Flyway 与演示数据迁移；不会删除项目文件。禁止对生产 Compose、生产数据库或生产凭据执行该脚本。
