# 阶段 7 仪表盘收尾记录

## 范围

阶段 7 在不改动阶段 5、阶段 6 已验证主流程的前提下，增加投递数据统计、Redis 缓存和 ECharts 仪表盘。后端接口统一挂在 `/api/v1/dashboard`：

| 接口 | 用途 |
| --- | --- |
| `GET /summary` | 总投递、进行中、待办、面试、Offer、转化率和逾期提醒 |
| `GET /funnel` | 已投递、测评、面试、Offer、接受 Offer 漏斗 |
| `GET /trends` | 按日、周、月统计投递趋势，并补齐零值桶 |
| `GET /sources` | 来源分布 |
| `GET /industries` | 行业分布 |
| `GET /cycle-time` | 投递到面试、Offer、终态的平均/最小/最大耗时 |
| `GET /upcoming` | 请求时间范围内未来 7 天最多 10 条面试 |

公共查询参数为 `startDate`、`endDate`、`timezone`、`companyId`、`source`、`includeArchived`；趋势额外使用 `granularity=DAY|WEEK|MONTH`。日期范围必须是合法日期且起止顺序正确，最长 367 天；时区必须是 IANA 时区。`companyId` 会先校验当前用户归属，越权返回 404。默认时区为 `Asia/Shanghai`，日期边界按用户时区解释，再转换为数据库使用的 Asia/Shanghai/UTC 时间。

## 指标口径

- 总投递按 `COALESCE(applied_at, created_at)` 进入日期范围统计，包含尚未投递的 `SAVED` 草稿；软删除不计入，默认不计归档。
- 进行中排除 `REJECTED`、`WITHDRAWN`、`ACCEPTED`、`ARCHIVED` 等终态和归档记录。
- 漏斗和转化使用 `jt_application_status_log` 的历史事实，因此跳过某个状态的投递仍可进入后续阶段；当前状态为 `OFFERED`/`ACCEPTED` 的记录也按历史阶段统计。
- 面试转化率为“有面试历史 / 有高级阶段历史”，Offer 转化率为“有 Offer 历史 / 有高级阶段历史”；分母为 0 时返回 0，不发生除零。
- 趋势按用户时区分桶，DAY/WEEK/MONTH 的缺失桶由服务层补 0；来源为空显示 `UNKNOWN`，行业为空显示 `未分类`。
- 周期时间只统计同时存在起点和终点历史的完整样本，单位为小时，不完整样本不参与平均值。
- `upcoming` 只查询未来 7 天内的面试，按开始时间升序，最多返回 10 条；返回时间转换为请求时区。

## 数据库和执行计划

Flyway `V5__dashboard_analytics_indexes.sql` 新增三个复合索引：

```sql
idx_app_dashboard_analytics
  (user_id, deleted, archived, company_id, source, applied_at, created_at)
idx_status_log_dashboard_analytics
  (user_id, deleted, to_status, application_id, changed_at)
idx_interview_dashboard_analytics
  (user_id, deleted, status, scheduled_start, application_id)
```

开发库 MySQL 8.4 的代表性 `EXPLAIN` 结果：

| 查询 | 使用索引 | rows | Extra |
| --- | --- | ---: | --- |
| 投递日期范围汇总 | `idx_app_dashboard_analytics` | 5 | `Using where; Using index` |
| 状态历史阶段聚合 | `idx_status_log_dashboard_analytics` | 3 | `Using index` |
| 未来面试查询 | `idx_interview_dashboard_analytics` | 4 | `Using where; Using index; Using filesort` |

查询都先按 `user_id` 和 `deleted` 隔离数据；最后一条的 `filesort` 只服务于最多 10 条的开始时间排序，后续数据量扩大时应继续观察执行计划。

## Redis 缓存

缓存键格式为：

```text
dashboard:v1:{userId}:{type}:{sha256(filter)[0:16]}:v{version}
```

例如：`dashboard:v1:1:summary:0867def4eac829f2:v0`。过滤条件序列化后计算 SHA-256，版本号放在 `dashboard:version:{userId}`，数据 TTL 为 120 秒，版本键 TTL 为 30 天。当前实现使用本地单飞锁，等待 200ms 未取得锁时直接回源数据库；Redis 连接、读写、序列化或反序列化异常均降级为数据库查询，不影响接口可用性。

应用、公司、面试的新增、修改、状态变化、归档、恢复、删除事务提交后，通过 `afterCommit` 递增当前用户的 dashboard 版本。旧键保留到自然过期，新请求使用新版本键，避免事务回滚时提前清除有效缓存。

## 真实验证记录

使用开发 Compose 的 MySQL 8.4、Redis 8 和打包后的 Spring Boot jar（8093 端口）验证：

1. 登录后依次请求 7 个接口，均返回 `SUCCESS`；摘要返回 `totalApplications=5`、`inProgressApplications=4`、`upcomingInterviews=4`、`interviewConversionRate=50.0`、`offerConversionRate=25.0`。
2. `trends` 请求 `2026-07-01` 至 `2026-08-31` 返回 62 个日桶，缺失日期返回 0；`cycle-time` 返回 2 个投递到面试样本、1 个投递到 Offer 样本和 1 个终态样本。
3. 同一过滤条件连续请求后，Redis 出现 summary/funnel/trends/sources/industries/cycle-time/upcoming 7 个键，观察到 TTL 约 106～108 秒。
4. 对现有公司执行字段不变的 PUT 后，`dashboard:version:1` 从默认 0 递增到 1；再次请求生成 `...:v1` 新版本缓存键，旧 `...:v0` 键不影响结果。
5. 临时停止 `jobtrack-redis` 后，摘要接口仍返回 HTTP 200、`SUCCESS`、`totalApplications=5`；随后已重新启动 Redis，容器状态恢复为 `running healthy`。

## 前端

`jobtrack-web/src/views/dashboard/DashboardView.vue` 提供日期范围、公司、来源、粒度和归档筛选；摘要卡片、漏斗、趋势、来源、行业、周期表格、即将到来的面试和未读通知分别加载。单个统计块失败只显示该块错误，不清空整页；ECharts 实例在窗口变化时 resize，在组件卸载时 dispose。

验证结果：`npm run type-check`、`npm run test`（4/4）和 `npm run build` 均通过。构建仍提示 dashboard chunk 约 565 kB（gzip 约 193 kB），这是 ECharts 首屏包体积的后续优化项，不影响本阶段功能。
